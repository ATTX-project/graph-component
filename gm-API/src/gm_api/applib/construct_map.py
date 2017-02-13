import threading
from gm_api.utils.db import connect_DB
from gm_api.applib.jsonld_index import LODResource
from gm_api.utils.logs import app_logger, thread_logger


class MappingObject(object):
    """Construct Mapping based on plugin and mapping specification."""

    @classmethod
    def create_map(cls, targetEndpoint, mapping, graphStore, plugin):
        """Create a map."""
        conn = connect_DB()
        status = "WIP"  # status for Work In Progress
        result = cls.register_map(conn, status, mapping)
        daemon_thread = threading.Thread(name='daemon', target=cls.daemon,
                                         args=(result, targetEndpoint, mapping, graphStore, plugin))
        daemon_thread.setDaemon(True)
        daemon_thread.start()
        app_logger.info('Construct map based on Map object.')
        return result

    @classmethod
    def retrieve_mapID(cls, mapID):
        """Retrieve map."""
        conn = connect_DB()
        result = cls.check_map_status(conn, mapID)
        app_logger.info('Retrieve map with the ID: {0}'.format(mapID))
        return result

    @classmethod
    def delete_mapID(cls, mapID):
        """Delete map."""
        conn = connect_DB()
        cls.delete_map(conn, mapID)
        app_logger.info('Delete map with the ID: {0}'.format(mapID))
        return

    @classmethod
    def daemon(cls, result, targetEndpoint, mapping, graphStore, plugin):
        """Simple worker daemon."""
        conn = connect_DB()
        ldmap_args = [targetEndpoint, graphStore, mapping['query'], mapping['context'], mapping['resourceType'], result['id'], mapping['index']]
        try:
            thread_logger.info('Starting Daemon thread.')
            if plugin == 'python':
                data = LODResource()
                data.map_jsonld(*ldmap_args)
                cls.update_map_status(conn, result['id'], "Done")
            elif plugin == 'java':
                conn = connect_DB()
                data = LODResource()
                cls.update_map_status(conn, result['id'], "Done")
                data.map_esJava('gc-rdf2json-indexer.jar', graphStore, targetEndpoint, mapping)
            else:
                return
            thread_logger.info('Exiting thread!')
        except Exception as error:
            app_logger.error('Thread Failed!')
            cls.update_map_status(conn, result['id'], str(error))
            return error

    @staticmethod
    def register_map(conn, status, mapping):
        """Create the map."""
        db_cursor = conn.cursor()
        # Insert a row of data
        db_cursor.execute("INSERT INTO maps VALUES (?, ?)", (status, str(mapping)))
        # Save (commit) the changes
        conn.commit()
        result = {'id': db_cursor.lastrowid, 'status': status}
        app_logger.info('Create row in the database with ID: {0}'.format(db_cursor.lastrowid))
        db_cursor.close()
        return result

    @staticmethod
    def check_map_status(conn, mapID):
        """Check the map status."""
        db_cursor = conn.cursor()
        # Insert a row of data
        for row in db_cursor.execute('SELECT rowid, status FROM maps WHERE rowid=?', (mapID, )):
            result = {'id': row[0], 'status': row[1]}
            app_logger.info('Check status in the database for ID: {0}'.format(mapID))
            break
        else:
            result = None
            app_logger.warning('Check status: There is no record for ID: {0}'.format(mapID))
        db_cursor.close()
        return result

    @staticmethod
    def delete_map(conn, mapID):
        """Detele the map data."""
        db_cursor = conn.cursor()
        # Delete a row of data
        db_cursor.execute('DELETE FROM maps WHERE rowid=?', (mapID, ))
        # Save (commit) the changes
        conn.commit()
        app_logger.info('Delete map in the database with ID: {0}'.format(mapID))
        db_cursor.close()
        return

    @staticmethod
    def update_map_status(conn, mapID, status):
        """Update the map status information."""
        db_cursor = conn.cursor()
        db_cursor.execute("SELECT rowid FROM maps WHERE rowid = ?", (mapID,))
        data = db_cursor.fetchone()
        if data is None:
            app_logger.info('There is no record for ID:'.format(mapID))
            pass
        else:
            db_cursor.execute('UPDATE maps SET status=? WHERE rowid=?', (status, mapID))
            # Save (commit) the changes
            conn.commit()
            app_logger.info('Update status in the database for ID: {0}'.format(mapID))
        db_cursor.close()
        return
