import sqlite3
from gm_api.utils.logs import app_logger


def connect_DB():
    """Connect to DB by parsing configuration."""
    try:
        conn = sqlite3.connect('data.db')
        app_logger.info('Connecting to database.')
        cursor = conn.cursor()
        cursor.execute("""create table if not exists maps (status text, data text, map text)""")
        app_logger.info('Creating maps table in the database.')
        return conn
    except Exception as error:
        app_logger.error('Connection Failed!\
            \nError Code is {0};\
            \nError Content is {1};'.format(error.args[0], error.args[1]))
        return error
