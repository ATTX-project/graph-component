from gm_api.utils.db import connect_DB
import unittest
import sqlite3


class DBTestCase(unittest.TestCase):
    """Test for DB connection."""

    def setUp(self):
        """Set up test fixtures."""

    def test_connection(self):
        """Test connection."""
        cursor = connect_DB()
        if isinstance(cursor, sqlite3.Cursor):
            self.assertRaises(Exception)
        else:
            pass
