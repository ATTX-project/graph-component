import threading
from gm_api.utils.db import connect_DB
from gm_api.applib.jsonld_index import LODResource
from gm_api.utils.logs import app_logger, thread_logger


class IndexingObject(object):
    """Construct Indexing based on plugin and indexing specification."""

    @classmethod
    def create_index(cls, targetEndpoint, indexing, graphStore, plugin):
        """Create a indexing job."""
        conn = connect_DB()
        status = "WIP"  # status for Work In Progress
        result = cls.register_index(conn, status, indexing)
        daemon_thread = threading.Thread(name='daemon', target=cls.daemon,
                                         args=(result, targetEndpoint, indexing, graphStore, plugin))
        daemon_thread.setDaemon(True)
        daemon_thread.start()
        app_logger.info('Construct index based on Map object.')
        return result

    @classmethod
    def retrieve_indexID(cls, indexID):
        """Retrieve index with ID."""
        conn = connect_DB()
        result = cls.check_index_status(conn, indexID)
        app_logger.info('Retrieve index with the ID: {0}'.format(indexID))
        return result

    @classmethod
    def delete_indexID(cls, indexID):
        """Delete index with ID."""
        conn = connect_DB()
        cls.delete_index(conn, indexID)
        app_logger.info('Delete index with the ID: {0}'.format(indexID))
        return

    @classmethod
    def daemon(cls, result, targetEndpoint, indexing, graphStore, plugin):
        """Simple worker daemon."""
        conn = connect_DB()
        try:
            thread_logger.info('Starting Daemon thread.')
            if plugin == 'python':
                ldindex_args = [targetEndpoint, graphStore, indexing['filter'], indexing['resourceType'], result['id'], indexing['index']]
                data = LODResource()
                data.index_jsonld(*ldindex_args)
                cls.update_index_status(conn, result['id'], "Done")
            elif plugin == 'java':
                conn = connect_DB()
                data = LODResource()
                cls.update_index_status(conn, result['id'], "Done")
                data.index_esJava('java/gc-rdf2json-indexer.jar', graphStore, targetEndpoint, indexing)
            else:
                return
            thread_logger.info('Exiting Daemon thread!')
        except Exception as error:
            app_logger.error('Daemon Thread Failed! with error: {0}'.format(error))
            cls.update_index_status(conn, result['id'], "Error")
            return error

    @staticmethod
    def register_index(conn, status, indexing):
        """Create the index."""
        db_cursor = conn.cursor()
        # Insert a row of data
        db_cursor.execute("INSERT INTO indexes VALUES (?, ?)", (status, str(indexing)))
        # Save (commit) the changes
        conn.commit()
        result = {'id': db_cursor.lastrowid, 'status': status}
        app_logger.info('Create row in the database with ID: {0}'.format(db_cursor.lastrowid))
        db_cursor.close()
        return result

    @staticmethod
    def check_index_status(conn, indexID):
        """Check the index status."""
        db_cursor = conn.cursor()
        # Insert a row of data
        for row in db_cursor.execute('SELECT rowid, status FROM indexes WHERE rowid=?', (indexID, )):
            result = {'id': row[0], 'status': row[1]}
            app_logger.info('Check status in the database for ID: {0}'.format(indexID))
            break
        else:
            result = None
            app_logger.warning('Check status: There is no record for ID: {0}'.format(indexID))
        db_cursor.close()
        return result

    @staticmethod
    def delete_index(conn, indexID):
        """Detele the index data."""
        db_cursor = conn.cursor()
        # Delete a row of data
        db_cursor.execute('DELETE FROM indexes WHERE rowid=?', (indexID, ))
        # Save (commit) the changes
        conn.commit()
        app_logger.info('Delete index in the database with ID: {0}'.format(indexID))
        db_cursor.close()
        return

    @staticmethod
    def update_index_status(conn, indexID, status):
        """Update the index status information."""
        db_cursor = conn.cursor()
        db_cursor.execute("SELECT rowid FROM indexes WHERE rowid = ?", (indexID,))
        data = db_cursor.fetchone()
        if data is None:
            app_logger.info('There is no record for ID:'.format(indexID))
            pass
        else:
            db_cursor.execute('UPDATE indexes SET status=? WHERE rowid=?', (status, indexID))
            # Save (commit) the changes
            conn.commit()
            app_logger.info('Update status in the database for ID: {0}'.format(indexID))
        db_cursor.close()
        return
