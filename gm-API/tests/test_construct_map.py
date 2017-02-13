import unittest
from mock import patch
from gm_api.applib.construct_map import MappingObject


class TestConstructMappings(unittest.TestCase):
    """Test Mapping Construction."""

    def setUp(self):
        """Set up test fixtures."""
        self.the_request = {"plugin": "python", "targetEndpoint": {"host": "localhost", "port": 9200},
                            "mapping": {"index": "default", "resourceType": "work",
                                        "query": "CONSTRUCT {?subject ?predicate ?object} WHERE { ?subject ?predicate ?object } LIMIT 25",
                                        "context": {"@context": "http://schema.org/"}
                                        },
                            "sourceGraphs": {"endpoint": {"host": "localhost", "port": 3030, "dataset": "ds"},
                                             "graphs": [
                                             "http://data.hulib.helsinki.fi/attx/ids",
                                             "http://data.hulib.helsinki.fi/attx/work1"]
                                             },
                            "format": "application/json+ld"}

    @patch.object(MappingObject, 'create_map')
    def test_cluster_initialization(self, create_map_mock):
        """Test MappingObject initialization."""
        new_map = MappingObject()
        new_map.create_map(self.the_request['targetEndpoint'],
                           self.the_request['mapping'],
                           self.the_request['sourceGraphs'], self.the_request['plugin'], self.the_request['format'])
        create_map_mock.assert_called_once_with(self.the_request['targetEndpoint'],
                                                self.the_request['mapping'],
                                                self.the_request['sourceGraphs'], self.the_request['plugin'], self.the_request['format'])
        assert new_map.create_map == create_map_mock


if __name__ == "__main__":
    unittest.main()
