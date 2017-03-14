import threading
from gm_api.utils.db import connect_DB
from gm_api.utils.logs import app_logger, thread_logger
from gm_api.applib.generate_links import determine_strategy


class LinkingObject(object):
    """Construct Linking job based on selected strategy and selected graphs."""

    @classmethod
    def create_links(cls, strategy, graphStore):
        """Create a linking job."""
        conn = connect_DB('data.db')
        status = "WIP"  # status for Work In Progress
        result = cls.register_link(conn, status, strategy)
        daemon_thread = threading.Thread(name='daemon', target=cls.daemon,
                                         args=(result, strategy, graphStore))
        daemon_thread.setDaemon(True)
        daemon_thread.start()
        app_logger.info('Construct linking job based on Strategy object.')
        return result

    @classmethod
    def retrieve_linkID(cls, linkID):
        """Retrieve linking job with ID."""
        conn = connect_DB('data.db')
        result = cls.check_link_status(conn, linkID)
        app_logger.info('Retrieved linking job with the ID: {0}'.format(linkID))
        return result

    @classmethod
    def delete_linkID(cls, linkID):
        """Delete linking job with ID."""
        conn = connect_DB('data.db')
        cls.delete_link(conn, linkID)
        app_logger.info('Delete linking job with the ID: {0}'.format(linkID))
        return

    @classmethod
    def daemon(cls, result, strategy, graphStore):
        """Simple worker daemon."""
        conn = connect_DB('data.db')
        try:
            thread_logger.info('Starting Daemon thread.')
            determine_strategy(graphStore, strategy)
            cls.update_link_status(conn, result['id'], "Done")
            thread_logger.info('Exiting Daemon thread!')
        except Exception as error:
            app_logger.error('Daemon Thread Failed! with error: {0}'.format(error))
            cls.update_link_status(conn, result['id'], "Error")
            return error

    @staticmethod
    def register_link(conn, status, strategy):
        """Create the linking job."""
        db_cursor = conn.cursor()
        # Insert a row of data
        db_cursor.execute("INSERT INTO linking VALUES (?, ?)", (status, str(strategy["uri"])))
        # Save (commit) the changes
        conn.commit()
        result = {'id': db_cursor.lastrowid, 'status': status}
        app_logger.info('Create row in the database with linking job ID: {0}'.format(db_cursor.lastrowid))
        db_cursor.close()
        return result

    @staticmethod
    def check_link_status(conn, linkID):
        """Check the linking job status."""
        db_cursor = conn.cursor()
        # Insert a row of data
        for row in db_cursor.execute('SELECT rowid, linkstatus FROM linking WHERE rowid=?', (linkID, )):
            result = {'id': row[0], 'status': row[1]}
            app_logger.info('Check status in the database for linking job ID: {0}'.format(linkID))
            break
        else:
            result = None
            app_logger.warning('Check status: There is no record for linking job ID: {0}'.format(linkID))
        db_cursor.close()
        return result

    @staticmethod
    def delete_link(conn, linkID):
        """Detele the linking job data."""
        db_cursor = conn.cursor()
        # Delete a row of data
        db_cursor.execute('DELETE FROM linking WHERE rowid=?', (linkID, ))
        # Save (commit) the changes
        conn.commit()
        app_logger.info('Delete linking job in the database with ID: {0}'.format(linkID))
        db_cursor.close()
        return

    @staticmethod
    def update_link_status(conn, linkID, status):
        """Update the linking job status information."""
        db_cursor = conn.cursor()
        db_cursor.execute("SELECT rowid FROM linking WHERE rowid = ?", (linkID,))
        data = db_cursor.fetchone()
        if data is None:
            app_logger.info('There is no record for linking job ID:'.format(linkID))
            pass
        else:
            db_cursor.execute('UPDATE linking SET linkstatus=? WHERE rowid=?', (status, linkID))
            # Save (commit) the changes
            conn.commit()
            app_logger.info('Update status in the database for linking job ID: {0}'.format(linkID))
        db_cursor.close()
        return
