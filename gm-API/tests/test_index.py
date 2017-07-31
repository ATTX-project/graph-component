import falcon
import unittest
import httpretty
from falcon import testing
from gm_api.app import init_api
from gm_api.app import api_version
from gm_api.utils.db import connect_DB


class appIndexTest(testing.TestCase):
    """Testing GM index function and initialize it for that purpose."""

    def setUp(self):
        """Setting the app up."""
        self.conn = connect_DB()
        self.app = init_api()

    def tearDown(self):
        """Tearing down the app up."""
        pass


class TestIndex(appIndexTest):
    """Testing GM index function and initialize it for that purpose."""

    def test_create(self):
        """Test GET index message."""
        self.app
        pass

    def test_index_post_bad(self):
        """Test empty POST index message."""
        hdrs = [('Accept', 'application/json'),
                ('Content-Type', 'application/json'), ]
        result = self.simulate_post('/%s/index' % (api_version), body='', headers=hdrs)
        assert(result.status == falcon.HTTP_400)

    @httpretty.activate
    def test_index_post_good(self):
        """Test good POST index message."""
        with open('tests/resources/index_request.json', 'r') as datafile:
            test_data = datafile.read().replace('\n', '')
        xml_result = """<?xml version="1.0"?>
                        <sparql xmlns="http://www.w3.org/2005/sparql-results#">
                            <head>
                                <variable name="dataset"/>
                            </head>
                            <results>
                          </results>
                        </sparql>"""
        httpretty.register_uri(httpretty.GET, "http://localhost:3030/ds/query", body=xml_result, status=200)
        hdrs = [('Accept', 'application/json'),
                ('Content-Type', 'application/json'), ]
        result = self.simulate_post('/%s/index' % (api_version), body=test_data, headers=hdrs)
        assert(result.status == falcon.HTTP_202)

    def test_index_post_good_java(self):
        """Test good POST index message."""
        with open('tests/resources/index_request_java.json', 'r') as datafile:
            test_data = datafile.read().replace('\n', '')
        hdrs = [('Accept', 'application/json'),
                ('Content-Type', 'application/json'), ]
        result = self.simulate_post('/%s/index' % (api_version), body=test_data, headers=hdrs)
        assert(result.status == falcon.HTTP_202)

    @httpretty.activate
    def test_index_get_good(self):
        """Test GET correct index data."""
        cur2 = self.conn.cursor()
        cur2.execute("INSERT INTO indexes VALUES (?, ?)", ("Done", 'smth'))
        self.conn.commit()
        created_id = cur2.lastrowid
        doc = {u'id': created_id, u'status': u'Done'}
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/index/%s" % (created_id), body=doc, status=200)
        result = self.simulate_get('/{0}/index/{1}'.format(api_version, created_id))
        assert(result.json == doc)
        cur2.execute('drop table if exists indexes')
        self.conn.commit()
        cur2.close()

    @httpretty.activate
    def test_index_get_ok(self):
        """Test GET index response is OK."""
        cur2 = self.conn.cursor()
        cur2.execute("INSERT INTO indexes VALUES (?, ?)", ("Done", 'smth'))
        self.conn.commit()
        created_id = cur2.lastrowid
        doc = {u'id': created_id, u'status': u'Done'}
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/index/1", body=doc, status=200)
        result = self.simulate_get('/{0}/index/{1}'.format(api_version, created_id))
        assert(result.status == falcon.HTTP_200)
        cur2.execute('drop table if exists indexes')
        self.conn.commit()
        cur2.close()

    @httpretty.activate
    def test_index_get_gone(self):
        """Test GET index response is Gone."""
        httpretty.register_uri(httpretty.GET, "http://localhost:4302/0.1/index/35", status=410)
        result = self.simulate_get('/{0}/index/{1}'.format(api_version, 35))
        assert(result.status_code == 410)

    @httpretty.activate
    def test_index_get_delete(self):
        """Test DELETE index response is OK."""
        created_id = 36
        httpretty.register_uri(httpretty.DELETE, "http://localhost:4302/0.1/index/36", status=200)
        result = self.simulate_delete('/{0}/index/{1}'.format(api_version, created_id))
        assert(result.status == falcon.HTTP_200)


if __name__ == "__main__":
    unittest.main()
