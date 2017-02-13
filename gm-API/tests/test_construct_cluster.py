import unittest
from mock import patch
from gm_api.applib.construct_cluster import ClusterID


class TestConstructCluster(unittest.TestCase):
    """Test Cluster Construction."""

    def setUp(self):
        """Set up test fixtures."""
        self.endpoint = {'host': 'localhost', 'port': 3030, 'dataset': 'ds'}

    @patch.object(ClusterID, 'cluster')
    def test_cluster_initialization(self, cluster_mock):
        """Test Cluster object initialization."""
        new_cluster = ClusterID()
        new_cluster.cluster(self.endpoint)
        cluster_mock.assert_called_once_with(self.endpoint)
        assert new_cluster.cluster == cluster_mock


if __name__ == "__main__":
    unittest.main()
