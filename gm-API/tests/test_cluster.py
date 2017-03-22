import falcon
import unittest
import httpretty
from falcon import testing
from gm_api.app import create
from gm_api.app import api_version
from gm_api.utils.db import connect_DB
from gm_api.utils.prefixes import ATTXProv, ATTXIDs


class appClusterTest(testing.TestCase):
    """Testing GM cluster function and initialize it for that purpose."""

    def setUp(self):
        """Setting the app up."""
        self.conn = connect_DB()
        self.app = create()

    def tearDown(self):
        """Tearing down the app up."""
        pass


class TestCluster(appClusterTest):
    """Testing GM cluster function and initialize it for that purpose."""

    def test_create(self):
        """Test GET map message."""
        self.app
        pass

    @httpretty.activate
    def test_cluster_post_bad(self):
        """Test bad POST cluster request."""
        with open('tests/resources/cluster_request.json', 'r') as datafile:
            test_data = datafile.read().replace('\n', '')
        xml_result = """<?xml version="1.0"?>
                        <sparql xmlns="http://www.w3.org/2005/sparql-results#">
                            <head>
                                <variable name="dataset"/>
                            </head>
                            <results>
                          </results>
                        </sparql>"""
        store_url = "http://localhost:3030/ds/query"
        query = """PREFIX prov: <http://www.w3.org/ns/prov#>
                    PREFIX attxonto: <http://data.hulib.helsinki.fi/attx/onto#>
                    SELECT DISTINCT ?dataset WHERE {
                    GRAPH <http://data.hulib.helsinki.fi/attx/prov> {
                    ?dataset a attxonto:Dataset.
                    ?activity prov:used|prov:generated ?dataset. }}"""
        httpretty.register_uri(httpretty.GET, "{0}?query={1}&output=xml&results=xml&format=xml".format(store_url, query), body=xml_result, status=200)
        httpretty.register_uri(httpretty.POST, "http://localhost:3030/ds/data?graph=%s" % (ATTXProv), status=500)
        httpretty.register_uri(httpretty.POST, "http://localhost:3030/ds/data?graph=%s" % (ATTXIDs), status=500)
        httpretty.register_uri(httpretty.POST, "http://localhost:4302/0.1/cluster", status=200)
        result = self.simulate_post('/%s/cluster' % (api_version), body=test_data)
        doc = {"status": "Error", "IDCount": 0}
        assert(result.json == doc)

    @httpretty.activate
    def test_cluster_post_good(self):
        """Test good POST cluster request."""
        with open('tests/resources/cluster_request.json', 'r') as datafile:
            test_data = datafile.read().replace('\n', '')
        xml_result = """<?xml version="1.0"?>
                        <sparql xmlns="http://www.w3.org/2005/sparql-results#">
                            <head>
                                <variable name="dataset"/>
                            </head>
                            <results>
                          </results>
                        </sparql>"""
        store_url = "http://localhost:3030/ds/query"
        query = """PREFIX prov: <http://www.w3.org/ns/prov#>
                    PREFIX attxonto: <http://data.hulib.helsinki.fi/attx/onto#>
                    SELECT DISTINCT ?dataset WHERE {
                    GRAPH <http://data.hulib.helsinki.fi/attx/prov> {
                    ?dataset a attxonto:Dataset.
                    ?activity prov:used|prov:generated ?dataset. }}"""
        httpretty.register_uri(httpretty.GET, "{0}?query={1}&output=xml&results=xml&format=xml".format(store_url, query), body=xml_result, status=200)
        httpretty.register_uri(httpretty.POST, "http://localhost:3030/ds/data?graph=%s" % (ATTXProv), status=200)
        httpretty.register_uri(httpretty.POST, "http://localhost:3030/ds/data?graph=%s" % (ATTXIDs), status=200)
        httpretty.register_uri(httpretty.POST, "http://localhost:4302/0.1/cluster", status=200)
        result = self.simulate_post('/%s/cluster' % (api_version), body=test_data)
        assert(result.status == falcon.HTTP_200)

    @httpretty.activate
    def test_cluster_post_message_error(self):
        """Test Unprocessable POST cluster message."""
        with open('tests/resources/cluster_request.json', 'r') as datafile:
            test_data = datafile.read().replace('\n', '')
        doc = {u'description': u'Could not process the clustering of ids.', u'title': u'Unprocessable ID Clustering'}
        httpretty.register_uri(httpretty.POST, "http://localhost:4302/0.1/cluster", body=doc, status=200)
        hdrs = [('Accept', 'application/json'),
                ('Content-Type', 'application/json'), ]
        result = self.simulate_post('/%s/cluster' % (api_version), body=test_data, headers=hdrs)
        assert(result.json == doc)


if __name__ == "__main__":
    unittest.main()
