import falcon
import requests
from urllib import quote
from datetime import datetime
from gm_api.utils.db import connect_DB
from gm_api.utils.logs import app_logger
from gm_api.utils.prefixes import ATTXProv


class UpdateProv(object):
    """Create Provenance class."""

    @classmethod
    def do_update(cls, graph_store, wf_endpoint, modifiedSince, start):
        """Do the update of the provenance information in the Graph Store."""
        conn = connect_DB()
        if start is True:
            started_id = cls.register_prov(conn, str(datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ")), "WIP")
            try:
                workflow = cls.get_provenance(wf_endpoint, 'workflow', modifiedSince)
                activity = cls.get_provenance(wf_endpoint, 'activity', modifiedSince)
                app_logger.info('Retrieving data fron WF-API finished.')
            except Exception:
                cls.update_prov_status(conn, started_id, "Error")
                app_logger.error('Failed to do the get provenance from WF-API update.')
                raise falcon.HTTPBadGateway(
                    'Failed to do the get provenance from WF-API update.'
                )
            try:
                if workflow.status_code == 304 and activity.status_code == 304:
                    cls.update_prov_status(conn, started_id, "Not Updated")
                    app_logger.info('Nothing to update in the Graph Store.')
                    pass
                elif workflow.status_code == 304:
                    cls.update_store(graph_store, activity, ATTXProv)
                    cls.update_prov_status(conn, started_id, "Done")
                    app_logger.info('Updating Activity information in the Graph Store')
                elif activity.status_code == 304:
                    cls.update_store(graph_store, workflow, ATTXProv)
                    cls.update_prov_status(conn, started_id, "Done")
                    app_logger.info('Updating Workflow information in the Graph Store')
                else:
                    cls.clean_store(graph_store, ATTXProv)
                    cls.update_store(graph_store, activity, ATTXProv)
                    cls.update_store(graph_store, workflow, ATTXProv)
                    cls.update_prov_status(conn, started_id, "Done")
                    app_logger.info('Updating Provenance in Graph Store Finished.')
                result = cls.check_prov_lastrow(conn)
                return {"lastStart": result[1], "status": result[0]}
            except Exception:
                cls.update_prov_status(conn, started_id, "Error")
                app_logger.error('Failed updating Provenance in Graph Store.')
                raise falcon.HTTPBadGateway(
                    'Failed to do Graph Store update.'
                )
        else:
            if cls.empty_prov_DB(conn) == 0:
                raise falcon.HTTPNotFound()
            else:
                result = cls.check_prov_lastrow(conn)
                return {"lastStart": result[1], "status": result[0]}

    @staticmethod
    def get_provenance(wf_endpoint, route, modifiedSince=None):
        """Get data from WF-API from a source."""
        source = "http://{0}:{1}/{2}".format(wf_endpoint["host"], wf_endpoint["port"], wf_endpoint["version"])
        if modifiedSince is None:
            wf_api = "{0}/{1}".format(source, route)
        else:
            wf_api = "{0}/{1}?modifiedSince={2}".format(source, route, quote(modifiedSince))
        result = requests.get(wf_api)
        if result.status_code in [500, 204]:
            app_logger.error('Endpoint {0} with the HTTP Response {1}.'.format(route, result.status_code))
            raise Exception
        else:
            app_logger.info('Endpoint {0} with the HTTP Response {1}.'.format(route, result.status_code))
            return result

    @staticmethod
    def update_store(graph_store, data, context=None):
        """Post data to a target Graph Store."""
        target = "http://{0}:{1}/{2}".format(graph_store["host"], graph_store["port"], graph_store["dataset"])
        if context is None:
            store_api = "{0}/data".format(target)
        else:
            store_api = "{0}/data?graph={1}".format(target, context)
        headers = {'Content-Type': 'text/turtle'}
        result = requests.post(store_api, data=data, headers=headers)
        app_logger.info('Endpoint {0} with the HTTP Response {1}.'.format(target, result.status_code))

    @staticmethod
    def clean_store(graph_store, context=None):
        """Clean Provenance Graph."""
        target = "http://{0}:{1}/{2}".format(graph_store["host"], graph_store["port"], graph_store["dataset"])
        if context is None:
            store_api = "{0}/data".format(target)
        else:
            store_api = "{0}/data?graph={1}".format(target, context)
        requests.delete(store_api)
        app_logger.info('Provenance Graph cleaned.')

    @staticmethod
    def register_prov(conn, date, status):
        """Create the map."""
        db_cursor = conn.cursor()
        # Insert a row of data
        db_cursor.execute("INSERT INTO prov VALUES (?, ?)", (status, date))
        # Save (commit) the changes
        conn.commit()
        result = db_cursor.lastrowid
        app_logger.info('Create row in the prov database with ID: {0}'.format(db_cursor.lastrowid))
        db_cursor.close()
        return result

    @staticmethod
    def empty_prov_DB(conn):
        """Check if the DB is empty."""
        db_cursor = conn.cursor()
        db_cursor.execute("""SELECT count(*) as count FROM prov""")
        result = db_cursor.fetchone()
        db_cursor.close()
        app_logger.info('Database has: {0} entries.'.format(result[0]))
        return result[0]

    @staticmethod
    def check_prov_lastrow(conn):
        """Retrieve the last result in prov database."""
        db_cursor = conn.cursor()
        db_cursor.execute("""SELECT * FROM prov ORDER BY rowid DESC LIMIT 1;""")
        result = db_cursor.fetchone()
        db_cursor.close()
        app_logger.info("The last job started at {0} and has status: {1}".format(result[1], result[0]))
        return result

    @staticmethod
    def update_prov_status(conn, rowid, status):
        """Update the map status information."""
        db_cursor = conn.cursor()
        db_cursor.execute("SELECT rowid FROM prov WHERE rowid = ?", (rowid,))
        data = db_cursor.fetchone()
        if data is None:
            app_logger.info('There is no record in prov database for ID:'.format(rowid))
            pass
        else:
            db_cursor.execute('UPDATE prov SET status=? WHERE rowid=?', (status, rowid))
            # Save (commit) the changes
            conn.commit()
            app_logger.info('Update status in the prov database for ID: {0}'.format(rowid))
        db_cursor.close()
        return
