import unittest
from rdflib import Graph
from gm_api.utils.prefixes import namspace_config, bind_prefix


class PrefixTestCase(unittest.TestCase):
    """Test for DB connection."""

    def setUp(self):
        """Set up test fixtures."""
        self.graph = Graph()

    def test_bind_prefix(self):
        """Test for Namespaces."""
        bind_prefix(self.graph)
        self.assertTrue(list(self.graph.namespaces()) != [], "Test if there are namespaces.")

    def test_namespace(self):
        """Test connection."""
        namspace_config('file.conf')
        pass
