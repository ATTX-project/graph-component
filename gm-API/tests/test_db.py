import unittest
import sqlite3
from sqlite3 import OperationalError
from mock import patch
from gm_api.utils.db import connect_DB, create_table


class DBTestCase(unittest.TestCase):
    """Test for DB connection."""

    def setUp(self):
        """Set up test fixtures."""

    def test_connection(self):
        """Test connection."""
        conn = connect_DB()
        self.assertTrue(isinstance(conn, sqlite3.Connection))

    @patch('gm_api.utils.db.create_table')
    def test_create_table(self, mock_function):
        """Create table."""
        conn = connect_DB()
        test = create_table(conn, """create table if not exists test (status text, start text)""")
        self.assertTrue(test is None)

    @patch('gm_api.utils.db.create_table')
    def test_create_table_error(self, mock_function):
        """Create table error."""
        conn = connect_DB()
        test = create_table(conn, """create table if not exists testing""")
        self.assertTrue(isinstance(test, OperationalError))
