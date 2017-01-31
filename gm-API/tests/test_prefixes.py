from gm_api.utils.prefixes import namspace_config
import unittest


class PrefixTestCase(unittest.TestCase):
    """Test for DB connection."""

    def setUp(self):
        """Set up test fixtures."""

    def test_namespace(self):
        """Test connection."""
        namspace_config('file.conf')
        pass
