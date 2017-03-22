import falcon
import unittest
import httpretty
from falcon import testing
from datetime import datetime
from gm_api.app import create
from gm_api.app import api_version
from gm_api.utils.db import connect_DB
from gm_api.utils.prefixes import ATTXProv


class appProvTest(testing.TestCase):
    """Testing GM prov function and initialize it for that purpose."""

    def setUp(self):
        """Setting the app up."""
        self.conn = connect_DB('tests/resources/test.db')
        self.app = create()

    def tearDown(self):
        """Tearing down the app up."""
        pass


class TestProv(appProvTest):
    """Testing GM app and initialize it for that purpose."""

    def test_create(self):
        """Test GET map message."""
        self.app
        pass

    @httpretty.activate
    def test_start_prov_ok(self):
        """Test GET start prov."""
        httpretty.register_uri(httpretty.GET, "http://localhost:4301/0.1/workflow", status=200)
        httpretty.register_uri(httpretty.GET, "http://localhost:4301/0.1/activity", status=200)
        httpretty.register_uri(httpretty.POST, "http://localhost:3030/ds/data?graph=%s" % (ATTXProv), status=200)
        httpretty.register_uri(httpretty.DELETE, "http://localhost:3030/ds/data?graph=%s" % (ATTXProv), status=200)
        doc = {"lastStart": str(datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ")), "status": "Done"}
        params = {"start": True}
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/prov?start=True", body=doc, status=200)
        result = self.simulate_get('/{0}/prov'.format(api_version), params=params)
        assert(result.status == falcon.HTTP_200)

    @httpretty.activate
    def test_start_prov_params(self):
        """Test GET start prov with parameters for hosts."""
        httpretty.register_uri(httpretty.GET, "http://localhost:4301/0.1/workflow", status=200)
        httpretty.register_uri(httpretty.GET, "http://localhost:4301/0.1/activity", status=200)
        httpretty.register_uri(httpretty.POST, "http://localhost:3030/ds/data?graph=%s" % (ATTXProv), status=200)
        httpretty.register_uri(httpretty.DELETE, "http://localhost:3030/ds/data?graph=%s" % (ATTXProv), status=200)
        doc = {"lastStart": str(datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ")), "status": "Done"}
        params = {"start": True, "wfapi": "http://localhost:4301/0.1", "graphStore": "http://localhost:3030/ds"}
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/prov?start=True&wfapi=localhost&graphStore=localhost", body=doc, status=200)
        result = self.simulate_get('/{0}/prov'.format(api_version), params=params)
        assert(result.status == falcon.HTTP_200)

    @httpretty.activate
    def test_start_prov_not_modified(self):
        """Test GET start prov not modified."""
        modifiedSince = str(datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ"))
        httpretty.register_uri(httpretty.GET, "http://localhost:4301/0.1/workflow?modifiedSince=%s" % (modifiedSince), status=304)
        httpretty.register_uri(httpretty.GET, "http://localhost:4301/0.1/activity?modifiedSince=%s" % (modifiedSince), status=304)
        params = {"start": True, "modifiedSince": modifiedSince}
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/prov?start=True", status=200)
        result = self.simulate_get('/{0}/prov'.format(api_version), params=params)
        assert(result.status == falcon.HTTP_200)

    @httpretty.activate
    def test_start_prov_act_not_modified(self):
        """Test GET start prov activities not modified."""
        modifiedSince = str(datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ"))
        httpretty.register_uri(httpretty.GET, "http://localhost:4301/0.1/workflow?modifiedSince=%s" % (modifiedSince), status=200)
        httpretty.register_uri(httpretty.GET, "http://localhost:4301/0.1/activity?modifiedSince=%s" % (modifiedSince), status=304)
        httpretty.register_uri(httpretty.POST, "http://localhost:3030/ds/data?graph=%s" % (ATTXProv), status=200)
        httpretty.register_uri(httpretty.DELETE, "http://localhost:3030/ds/data?graph=%s" % (ATTXProv), status=200)
        params = {"start": True, "modifiedSince": modifiedSince}
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/prov?start=True", status=200)
        result = self.simulate_get('/{0}/prov'.format(api_version), params=params)
        assert(result.status == falcon.HTTP_200)

    @httpretty.activate
    def test_start_prov_wrf_not_modified(self):
        """Test GET start prov workflows not modified."""
        modifiedSince = str(datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ"))
        httpretty.register_uri(httpretty.GET, "http://localhost:4301/0.1/workflow?modifiedSince=%s" % (modifiedSince), status=304)
        httpretty.register_uri(httpretty.GET, "http://localhost:4301/0.1/activity?modifiedSince=%s" % (modifiedSince), status=200)
        httpretty.register_uri(httpretty.POST, "http://localhost:3030/ds/data?graph=%s" % (ATTXProv), status=200)
        httpretty.register_uri(httpretty.DELETE, "http://localhost:3030/ds/data?graph=%s" % (ATTXProv), status=200)
        params = {"start": True, "modifiedSince": modifiedSince}
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/prov?start=True", status=200)
        result = self.simulate_get('/{0}/prov'.format(api_version), params=params)
        assert(result.status == falcon.HTTP_200)

    @httpretty.activate
    def test_map_prov_no_wf(self):
        """Test GET start prov with no WF-API."""
        httpretty.register_uri(httpretty.POST, "http://localhost:3030/ds/data?graph=%s" % (ATTXProv), status=200)
        httpretty.register_uri(httpretty.DELETE, "http://localhost:3030/ds/data?graph=%s" % (ATTXProv), status=200)
        params = {"start": True}
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/prov?start=True", status=200)
        result = self.simulate_get('/{0}/prov'.format(api_version), params=params)
        assert(result.status == falcon.HTTP_502)

    @httpretty.activate
    def test_start_prov_no_graph(self):
        """Test GET start prov."""
        httpretty.register_uri(httpretty.GET, "http://localhost:4301/0.1/workflow", status=200)
        httpretty.register_uri(httpretty.GET, "http://localhost:4301/0.1/activity", status=200)
        params = {"start": True}
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/prov?start=True", status=200)
        result = self.simulate_get('/{0}/prov'.format(api_version), params=params)
        assert(result.status == falcon.HTTP_502)

    @httpretty.activate
    def test_get_prov(self):
        """Test GET prov response."""
        cur2 = self.conn.cursor()
        cur2.execute("INSERT INTO prov VALUES (?, ?)", ("Test", str(datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ"))))
        self.conn.commit()
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/prov", status=200)
        result = self.simulate_get('/{0}/prov'.format(api_version))
        assert(result.status == falcon.HTTP_200)
        cur2.execute('drop table if exists prov')
        self.conn.commit()
        cur2.close


if __name__ == "__main__":
    unittest.main()
