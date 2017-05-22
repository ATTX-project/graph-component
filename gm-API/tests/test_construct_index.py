import unittest
from mock import patch
from gm_api.applib.construct_index import IndexingObject


class TestConstructIndexing(unittest.TestCase):
    """Test Indexing Construction."""

    def setUp(self):
        """Set up test fixtures."""
        self.the_request = {"plugin": "python", "targetEndpoint": {"host": "localhost", "port": 9200},
                            "indexing": {"index": "default", "resourceType": "work",
                                         "filter": "CONSTRUCT {?subject ?predicate ?object} WHERE { ?subject ?predicate ?object } LIMIT 25"
                                         },
                            "sourceGraphs": {"endpoint": {"host": "localhost", "port": 3030, "dataset": "ds"},
                                             "graphs": [
                                             "http://data.hulib.helsinki.fi/attx/ids",
                                             "http://data.hulib.helsinki.fi/attx/work1"]
                                             },
                            "format": "application/json+ld"}

    @patch.object(IndexingObject, 'create_index')
    def test_cluster_initialization(self, create_index_mock):
        """Test IndexingObject initialization."""
        new_index = IndexingObject()
        new_index.create_index(self.the_request['targetEndpoint'],
                               self.the_request['indexing'],
                               self.the_request['sourceGraphs'], self.the_request['plugin'], self.the_request['format'])
        create_index_mock.assert_called_once_with(self.the_request['targetEndpoint'],
                                                  self.the_request['indexing'],
                                                  self.the_request['sourceGraphs'], self.the_request['plugin'], self.the_request['format'])
        assert new_index.create_index == create_index_mock


if __name__ == "__main__":
    unittest.main()
