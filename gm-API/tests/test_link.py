import falcon
import unittest
import httpretty
from falcon import testing
from gm_api.app import create
from gm_api.app import api_version
from gm_api.utils.db import connect_DB


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

    def test_link_post_good(self):
        """Test good POST entities links message."""
        with open('tests/resources/links_request.json', 'r') as datafile:
            test_data = datafile.read().replace('\n', '')
        xml_result = """<?xml version="1.0"?>
                        <sparql xmlns="http://www.w3.org/2005/sparql-results#">
                          <head>
                            <variable name="property"/>
                            <variable name="value"/>
                          </head>
                          <results>
                          </results>
                        </sparql>"""
        httpretty.register_uri(httpretty.GET, "http://localhost:3030/ds/query", body=xml_result, status=200)
        hdrs = [('Accept', 'application/json'),
                ('Content-Type', 'application/json'), ]
        result = self.simulate_post('/%s/link' % (api_version), body=test_data, headers=hdrs)
        assert(result.status == falcon.HTTP_202)

    def test_link_generate_post_good(self):
        """Test good POST entities links message with generation of ids."""
        with open('tests/resources/link_request_generate.json', 'r') as datafile:
            test_data = datafile.read().replace('\n', '')
        hdrs = [('Accept', 'application/json'),
                ('Content-Type', 'application/json'), ]
        result = self.simulate_post('/%s/link' % (api_version), body=test_data, headers=hdrs)
        assert(result.status == falcon.HTTP_202)

    def test_link_post_bad(self):
        """Test empty POST links message."""
        hdrs = [('Accept', 'application/json'),
                ('Content-Type', 'application/json'), ]
        result = self.simulate_post('/%s/link' % (api_version), body='', headers=hdrs)
        assert(result.status == falcon.HTTP_400)

    @httpretty.activate
    def test_link_get_good(self):
        """Test GET correct link data."""
        cur2 = self.conn.cursor()
        cur2.execute("INSERT INTO linking VALUES (?, ?)", ("Done", 'smth'))
        self.conn.commit()
        created_id = cur2.lastrowid
        doc = {u'id': created_id, u'status': u'Done'}
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/link/%s" % (created_id), body=doc, status=200)
        result = self.simulate_get('/{0}/link/{1}'.format(api_version, created_id))
        assert(result.json == doc)
        cur2.execute('drop table if exists linking')
        self.conn.commit()
        cur2.close()

    @httpretty.activate
    def test_link_get_ok(self):
        """Test GET link response is OK."""
        cur2 = self.conn.cursor()
        cur2.execute("INSERT INTO linking VALUES (?, ?)", ("Done", 'smth'))
        self.conn.commit()
        created_id = cur2.lastrowid
        doc = {u'id': created_id, u'status': u'Done'}
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/link/1", body=doc, status=200)
        result = self.simulate_get('/{0}/link/{1}'.format(api_version, created_id))
        assert(result.status == falcon.HTTP_200)
        cur2.execute('drop table if exists linking')
        self.conn.commit()
        cur2.close()

    @httpretty.activate
    def test_link_get_gone(self):
        """Test GET link response is Gone."""
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/link/135", status=410)
        result = self.simulate_get('/{0}/link/{1}'.format(api_version, 135))
        assert(result.status_code == 410)

    @httpretty.activate
    def test_link_get_delete(self):
        """Test DELETE index response is OK."""
        created_id = 36
        httpretty.register_uri(httpretty.DELETE, "http://localhost:4302/0.1/link/36", status=200)
        result = self.simulate_delete('/{0}/link/{1}'.format(api_version, created_id))
        assert(result.status == falcon.HTTP_200)


if __name__ == "__main__":
    unittest.main()
