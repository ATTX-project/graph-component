import falcon
import unittest
import httpretty
import requests
from falcon import testing
from datetime import datetime
from gm_api.app import create
from gm_api.app import api_version
# from gm_api.utils.db import connect_DB
# from mock import MagicMock


class appTest(testing.TestCase):
    """Testing GM app and initialize it for that purpose."""

    def setUp(self):
        """Setting the app up."""
        self.app = create()

    def tearDown(self):
        """Tearing down the app up."""
        pass


class TestMyApp(appTest):
    """Testing GM app and initialize it for that purpose."""

    def test_create(self):
        """Test GET map message."""
        self.app
        pass

    def test_map_post_bad(self):
        """Test empty POST map message."""
        hdrs = [('Accept', 'application/json'),
                ('Content-Type', 'application/json'), ]
        result = self.simulate_post('/%s/map' % (api_version), body='', headers=hdrs)
        assert(result.status == falcon.HTTP_400)

    def test_map_post_good(self):
        """Test good POST map message."""
        with open('tests/resources/map_request.json', 'r') as datafile:
            test_data = datafile.read().replace('\n', '')
        hdrs = [('Accept', 'application/json'),
                ('Content-Type', 'application/json'), ]
        result = self.simulate_post('/%s/map' % (api_version), body=test_data, headers=hdrs)
        assert(result.status == falcon.HTTP_202)

    @httpretty.activate
    def test_cluster_post_good(self):
        """Test good POST cluster message."""
        httpretty.register_uri(httpretty.POST, "http://localhost:4302/0.1/cluster", status=200)
        result = requests.post('http://localhost:4302/0.1/cluster')
        assert(result.status_code == 200)

    def test_getlinks_post_good(self):
        """Test good POST entities links message."""
        with open('tests/resources/links_request.json', 'r') as datafile:
            test_data = datafile.read().replace('\n', '')
        hdrs = [('Accept', 'application/json'),
                ('Content-Type', 'application/json'), ]
        result = self.simulate_post('/%s/link' % (api_version), body=test_data, headers=hdrs)
        assert(result.status == falcon.HTTP_202)

    def test_getlinks_post_bad(self):
        """Test empty POST links message."""
        hdrs = [('Accept', 'application/json'),
                ('Content-Type', 'application/json'), ]
        result = self.simulate_post('/%s/link' % (api_version), body='', headers=hdrs)
        assert(result.status == falcon.HTTP_400)

    def test_getlinks_get_good(self):
        """Test GET entities links message."""
        result = self.simulate_get('/%s/link' % (api_version))
        assert(result.status == falcon.HTTP_200)

    @httpretty.activate
    def test_map_get_good(self):
        """Test GET correct map data."""
        created_id = 34
        doc = {u'id': created_id, u'status': u'Done'}
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/map/34", body=doc, status=200)
        result = self.simulate_get('/{0}/map/{1}'.format(api_version, created_id))
        assert(result.json == doc)

    @httpretty.activate
    def test_map_get_ok(self):
        """Test GET map response is OK."""
        created_id = 34
        doc = {u'id': created_id, u'status': u'Done'}
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/map/34", body=doc, status=200)
        result = self.simulate_get('/{0}/map/{1}'.format(api_version, created_id))
        assert(result.status == falcon.HTTP_200)

    @httpretty.activate
    def test_map_get_gone(self):
        """Test GET map response is OK."""
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/map/35", status=410)
        result = requests.get('http://localhost:4302/0.1/map/35')
        assert(result.status_code == 410)

    @httpretty.activate
    def test_map_get_delete(self):
        """Test DELETE map response is OK."""
        created_id = 36
        httpretty.register_uri(httpretty.DELETE, "http://localhost:4302/0.1/map/36", status=200)
        result = self.simulate_delete('/{0}/map/{1}'.format(api_version, created_id))
        assert(result.status == falcon.HTTP_200)

    @httpretty.activate
    def test_map_get_start_prov(self):
        """Test GET start prov."""
        doc = {"lastStart": str(datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ")), "status": "Done"}
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/prov?start=true", body=doc, status=200)
        result = requests.get('http://localhost:4302/0.1/prov?start=true')
        assert(result.status_code == 200)

    @httpretty.activate
    def test_map_get_prov(self):
        """Test GET prov response."""
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/prov", status=200)
        result = self.simulate_get('/{0}/prov'.format(api_version))
        assert(result.status == falcon.HTTP_200)


if __name__ == "__main__":
    unittest.main()
