import re
import json
import unittest
import httpretty
from falcon import testing
from rdflib import Graph
from gm_api.app import create
from gm_api.applib.generate_links import perform_strategy, output_generated_graph
from gm_api.utils.db import connect_DB
from gm_api.utils.prefixes import ATTXStrategy, ATTXBase


class appGenerateLinkTest(testing.TestCase):
    """Testing GM link function and initialize it for that purpose."""

    def setUp(self):
        """Setting the app up."""
        self.conn = connect_DB()
        self.app = create()

    def tearDown(self):
        """Tearing down the app up."""
        pass


class TestGenerateLink(appGenerateLinkTest):
    """Testing GM link function and initialize it for that purpose."""

    def test_create(self):
        """Test GET map message."""
        self.app
        pass

    @httpretty.activate
    def test_perform_strategy(self):
        """Test Update Linking provenance."""
        with open('tests/resources/links_request.json') as datafile:
            test_data = json.load(datafile)
        with open('tests/resources/xml_strategy.xml') as datafile:
            xml_result = datafile.read()
        store_url = "http://localhost:3030/ds/query"
        query = """PREFIX attxonto: <http://data.hulib.helsinki.fi/attx/onto#>
                   PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                   SELECT ?property ?value
                   WHERE { GRAPH <%s> {
                   <%s%s> ?property ?value .
                   filter ( ?property not in ( rdf:type ) )
                   }}""" % (ATTXStrategy, ATTXBase, re.split(r'\/|\#', test_data['strategy']['uri'])[-1])
        httpretty.register_uri(httpretty.GET, "{0}?query={1}&output=xml&results=xml&format=xml".format(store_url, query), xml_result, status=200)
        httpretty.register_uri(httpretty.POST, "http://localhost:3030/ds/data?graph=%s" % (re.split(r'\/|\#', test_data['strategy']['uri'])[-1]), status=200)
        current_output = perform_strategy(test_data['graphStore'], test_data['strategy'])
        assert(current_output == test_data['strategy']['output'])

    @httpretty.activate
    def test_output_generated_graph(self):
        """Test output generated Linking graph."""
        with open('tests/resources/links_request.json') as datafile:
            test_data = json.load(datafile)
        graph = Graph()
        graph.parse('tests/resources/links_result.ttl', format="turtle")
        httpretty.register_uri(httpretty.POST, "http://localhost:3030/ds/data?graph=%s" % (test_data['strategy']['output']))
        result_status = output_generated_graph(test_data['graphStore']['endpoint'], graph, test_data['strategy']['output'])
        assert(result_status == 200)


if __name__ == "__main__":
    unittest.main()
