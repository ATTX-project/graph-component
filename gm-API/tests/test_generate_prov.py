import unittest
import httpretty
from falcon import testing
from gm_api.app import create
from gm_api.applib.generate_prov import update_linking_provenance
from gm_api.utils.db import connect_DB
from gm_api.utils.prefixes import ATTXProv, ATTXURL


class appGenerateProvTest(testing.TestCase):
    """Testing GM link function and initialize it for that purpose."""

    def setUp(self):
        """Setting the app up."""
        self.conn = connect_DB()
        self.app = create()

    def tearDown(self):
        """Tearing down the app up."""
        pass


class TestGenerateProv(appGenerateProvTest):
    """Testing GM link function and initialize it for that purpose."""

    def test_create(self):
        """Test GET map message."""
        self.app
        pass

    @httpretty.activate
    def test_update_linking_provenance(self):
        """Test Update Linking provenance."""
        httpretty.register_uri(httpretty.POST, "http://localhost:3030/ds/data?graph=%s" % (ATTXProv), status=200)
        generatedDataset = "{0}work4".format(ATTXURL)
        usedDatasetList = ["http://data.hulib.helsinki.fi/attx/work2", "http://data.hulib.helsinki.fi/attx/work1"]
        endpoint = {'host': "localhost", 'port': 3030, 'dataset': 'ds'}
        strategy = 'http://data.hulib.helsinki.fi/attx/idstrategy1'
        result = update_linking_provenance(endpoint, "2016-11-17T13:02:10+02:00", "2016-11-17T13:40:47+02:00", strategy, generatedDataset, usedDatasetList)
        assert(result == 200)


if __name__ == "__main__":
    unittest.main()
