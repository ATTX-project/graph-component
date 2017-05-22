import unittest
from mock import patch
from gm_api.applib.update_prov import UpdateProv


class TestUpdateProv(unittest.TestCase):
    """Test Update Provenance."""

    def setUp(self):
        """Set up test fixtures."""
        self.graph_endpoint = {'host': 'localhost', 'port': 3030, 'dataset': 'ds'}
        self.wf_endpoint = {'host': 'localhost', 'port': 4301, 'version': "0.1"}
        self.modifiedSince = '2017-01-03T08:14:14Z'
        self.start = True

    @patch.object(UpdateProv, 'do_update')
    def test_cluster_initialization(self, do_update_mock):
        """Test UpdateProv initialization."""
        prov_update = UpdateProv()
        prov_update.do_update(self.graph_endpoint, self.wf_endpoint, self.modifiedSince, self.start)
        do_update_mock.assert_called_once_with(self.graph_endpoint, self.wf_endpoint, self.modifiedSince, self.start)
        assert prov_update.do_update == do_update_mock


if __name__ == "__main__":
    unittest.main()
