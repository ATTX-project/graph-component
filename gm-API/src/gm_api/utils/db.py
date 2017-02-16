import sqlite3
from gm_api.utils.logs import app_logger


def connect_DB(db_file=None):
    """Connect to DB by parsing configuration."""
    db_filename = ''
    if db_file is None:
        db_filename = 'data.db'
    else:
        db_filename = db_file
    try:
        conn = sqlite3.connect(db_filename)
        app_logger.info('Connecting to database.')
        create_table(conn, """create table if not exists indexes (status text, filter text)""")
        # create_table(conn, """create table if not exists graphstore (host text, port integer, dataset text)""")
        # create_table(conn, """create table if not exists esstore (host text, port integer, index text, resourcetype text)""")
        create_table(conn, """create table if not exists prov (status text, start text)""")
        app_logger.info('Creating tables in the database.')
        return conn
    except Exception as error:
        app_logger.error('Connection Failed!\
            \nError Code is {0};\
            \nError Content is {1};'.format(error.args[0], error.args[1]))
        return error


def create_table(conn, create_table_sql):
    """Create a table from the create_table_sql statement.

    :param conn: Connection object
    :param create_table_sql: a CREATE TABLE statement
    :return:
    """
    try:
        db_cursor = conn.cursor()
        db_cursor.execute(create_table_sql)
    except Exception as error:
        app_logger.error('Error {0}'.format(error))
        return error
