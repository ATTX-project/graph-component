import json
import falcon
import unittest
import httpretty
from falcon import testing
from gm_api.app import create
from gm_api.app import api_version
from gm_api.utils.db import connect_DB
from gm_api.utils.prefixes import ATTXStrategy, ATTXBase


class appLinkTest(testing.TestCase):
    """Testing GM link function and initialize it for that purpose."""

    def setUp(self):
        """Setting the app up."""
        self.conn = connect_DB()
        self.app = create()

    def tearDown(self):
        """Tearing down the app up."""
        pass


class TestLink(appLinkTest):
    """Testing GM link function and initialize it for that purpose."""

    def test_create(self):
        """Test GET map message."""
        self.app
        pass

    @httpretty.activate
    def test_linkstrategy_notfound(self):
        """Test GET link strategy. No strategies found."""
        xml_result = """<?xml version="1.0"?>
        <sparql xmlns="http://www.w3.org/2005/sparql-results#">
          <head>
            <variable name="property"/>
            <variable name="value"/>
          </head>
          <results>
          </results>
        </sparql>"""
        store_url = "http://localhost:3030/ds/query"
        query = """PREFIX attxonto: <http://data.hulib.helsinki.fi/attx/onto#>
                   PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                   SELECT ?property ?value
                   WHERE { GRAPH <%s> {
                   <%s%s> ?property ?value .
                   filter ( ?property not in ( rdf:type ) )
                   }}""" % (ATTXStrategy, ATTXBase, "idstrategy1")
        httpretty.register_uri(httpretty.GET, "{0}?query={1}&output=xml&results=xml&format=xml".format(store_url, query), xml_result, status=200)
        httpretty.register_uri(httpretty.POST, "http://localhost:3030/ds/data?graph=%s" % (ATTXStrategy), status=200)
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/linkstrategy/idstrategy1", status=404)
        result = self.simulate_get('/{0}/linkstrategy/idstrategy1'.format(api_version))
        assert(result.status == falcon.HTTP_404)

    @httpretty.activate
    def test_linkstrategies_notfound(self):
        """Test GET linkstrategies. No strategies found."""
        xml_result = """<?xml version="1.0"?>
        <sparql xmlns="http://www.w3.org/2005/sparql-results#">
          <head>
            <variable name="subject"/>
          </head>
          <results>
          </results>
        </sparql>"""
        store_url = "http://localhost:3030/ds/query"
        query = """PREFIX attxonto: <http://data.hulib.helsinki.fi/attx/onto#>
                    SELECT DISTINCT ?subject
                    WHERE { GRAPH <%s> {
                    ?subject a attxonto:LinkStrategy.
                    }}""" % (ATTXStrategy)
        httpretty.register_uri(httpretty.GET, "{0}?query={1}&output=xml&results=xml&format=xml".format(store_url, query), xml_result, status=200)
        httpretty.register_uri(httpretty.POST, "http://localhost:3030/ds/data?graph=%s" % (ATTXStrategy), status=200)
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/linkstrategy", status=404)
        result = self.simulate_get('/{0}/linkstrategy'.format(api_version))
        assert(result.status == falcon.HTTP_404)

    @httpretty.activate
    def test_linkstrategies_ok(self):
        """Test GET linkstrategies."""
        xml_result = """<?xml version="1.0"?>
            <sparql xmlns="http://www.w3.org/2005/sparql-results#">
              <head>
                <variable name="subject"/>
              </head>
              <results>
                <result>
                  <binding name="subject">
                    <uri>http://data.hulib.helsinki.fi/attx/idstrategy1</uri>
                  </binding>
                </result>
                <result>
                  <binding name="subject">
                    <uri>http://data.hulib.helsinki.fi/attx/idstrategy2</uri>
                  </binding>
                </result>
              </results>
            </sparql>"""
        store_url = "http://localhost:3030/ds/query"
        query = """PREFIX attxonto: <http://data.hulib.helsinki.fi/attx/onto#>
                    SELECT DISTINCT ?subject
                    WHERE { GRAPH <%s> {
                    ?subject a attxonto:LinkStrategy.
                    }}""" % (ATTXStrategy)
        httpretty.register_uri(httpretty.GET, "{0}?query={1}&output=xml&results=xml&format=xml".format(store_url, query), xml_result, status=200)
        httpretty.register_uri(httpretty.POST, "http://localhost:3030/ds/data?graph=%s" % (ATTXStrategy), status=200)
        doc = """[{"uri": "http://data.hulib.helsinki.fi/attx/idstrategy1"}, {"uri": "http://data.hulib.helsinki.fi/attx/idstrategy2"}]"""
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/linkstrategy", body=doc, status=200)
        result = self.simulate_get('/{0}/linkstrategy'.format(api_version))
        assert(result.status == falcon.HTTP_200)
        assert(result.json == json.loads(doc))

    @httpretty.activate
    def test_linkstrategy_ok(self):
        """Test GET link strategy."""
        xml_result = """<?xml version="1.0"?>
                <sparql xmlns="http://www.w3.org/2005/sparql-results#">
                  <head>
                    <variable name="property"/>
                    <variable name="value"/>
                  </head>
                  <results>
                    <result>
                      <binding name="property">
                        <uri>http://data.hulib.helsinki.fi/attx/onto#hasStrategyType</uri>
                      </binding>
                      <binding name="value">
                        <literal>SPARQL</literal>
                      </binding>
                    </result>
                    <result>
                      <binding name="property">
                        <uri>http://purl.org/dc/terms/title</uri>
                      </binding>
                      <binding name="value">
                        <literal>IDs based Linking Strategy</literal>
                      </binding>
                    </result>
                    <result>
                      <binding name="property">
                        <uri>http://schema.org/query</uri>
                      </binding>
                      <binding name="value">
                        <literal>prefix skos: &lt;http://www.w3.org/2004/02/skos/core#&gt;&#x0A; prefix attx: &lt;http://data.hulib.helsinki.fi/attx/&gt;&#x0A; construct { ?r1 skos:exactMatch ?r2} where { ?r1 attx:id ?id .&#x0A;  ?r2 attx:id ?id .&#x0A;  filter(?r1 != ?r2)&#x0A;}</literal>
                      </binding>
                    </result>
                    <result>
                      <binding name="property">
                        <uri>http://data.hulib.helsinki.fi/attx/onto#hasParameters</uri>
                      </binding>
                      <binding name="value">
                        <literal>List of parameters</literal>
                      </binding>
                    </result>
                    <result>
                      <binding name="property">
                        <uri>http://purl.org/dc/terms/description</uri>
                      </binding>
                      <binding name="value">
                        <literal xml:lang="en">Linking Strategy based on IDs clustering and specifying required datasets.</literal>
                      </binding>
                    </result>
                  </results>
                </sparql>"""
        store_url = "http://localhost:3030/ds/query"
        query = """PREFIX attxonto: <http://data.hulib.helsinki.fi/attx/onto#>
                   PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                   SELECT ?property ?value
                   WHERE { GRAPH <%s> {
                   <%s%s> ?property ?value .
                   filter ( ?property not in ( rdf:type ) )
                   }}""" % (ATTXStrategy, ATTXBase, "idstrategy1")
        httpretty.register_uri(httpretty.GET, "{0}?query={1}&output=xml&results=xml&format=xml".format(store_url, query), xml_result, status=200)
        httpretty.register_uri(httpretty.POST, "http://localhost:3030/ds/data?graph=%s" % (ATTXStrategy), status=200)
        with open('tests/resources/strategy_result.json', 'r') as datafile:
            doc = datafile.read()
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/linkstrategy/idstrategy1", body=doc, status=200)
        result = self.simulate_get('/{0}/linkstrategy/idstrategy1'.format(api_version))
        assert(result.status == falcon.HTTP_200)
        assert(result.json == json.loads(doc))


if __name__ == "__main__":
    unittest.main()
