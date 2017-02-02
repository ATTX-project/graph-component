from gm_api.app import create
import unittest
from falcon import testing
import falcon
from gm_api.utils.db import connect_DB


class appTest(testing.TestCase):
    """Testing GM app and initialize it for that purpose."""

    def setUp(self):
        """Setting the app up."""
        super(appTest, self).setUp()
        # Assume the hypothetical `myapp` package has a
        # function called `create()` to initialize and
        # return a `falcon.API` instance.
        self.app = create()


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
        result = self.simulate_post('/map', body='', headers=hdrs)
        assert(result.status == falcon.HTTP_400)

    def test_map_post_good(self):
        """Test good POST map message."""
        with open('tests/resources/map_request.json', 'r') as datafile:
            test_data = datafile.read().replace('\n', '')
        # test_data = open('tests/resources/map_request.json', 'r')
        hdrs = [('Accept', 'application/json'),
                ('Content-Type', 'application/json'), ]
        result = self.simulate_post('/map', body=test_data, headers=hdrs)
        assert(result.status == falcon.HTTP_202)

    def test_cluster_post_good(self):
        """Test good POST cluster message."""
        result = self.simulate_post('/clusterids')
        assert(result.status == falcon.HTTP_200)

    def test_getlinks_post_good(self):
        """Test good POST entities links message."""
        with open('tests/resources/links_request.json', 'r') as datafile:
            test_data = datafile.read().replace('\n', '')
        # test_data = open('tests/resources/map_request.json', 'r')
        hdrs = [('Accept', 'application/json'),
                ('Content-Type', 'application/json'), ]
        result = self.simulate_post('/links', body=test_data, headers=hdrs)
        assert(result.status == falcon.HTTP_202)

    def test_getlinks_post_bad(self):
        """Test empty POST links message."""
        hdrs = [('Accept', 'application/json'),
                ('Content-Type', 'application/json'), ]
        result = self.simulate_post('/links', body='', headers=hdrs)
        assert(result.status == falcon.HTTP_400)

    def test_getlinks_get_good(self):
        """Test GET entities links message."""
        result = self.simulate_get('/links')
        assert(result.status == falcon.HTTP_200)

    def test_map_get_good(self):
        """Test GET correct map data."""
        conn = connect_DB()
        db_cursor = conn.cursor()
        # Insert a row of data
        db_cursor.execute("insert into maps values (?, ?, ?)", ('Done', '', ''))
        # Save (commit) the changes
        created_id = db_cursor.lastrowid
        conn.commit()
        doc = {u'id': created_id, u'status': u'Done'}
        result = self.simulate_get('/map/{0}'.format(created_id))
        assert(result.json == doc)
        conn.close()

    def test_map_get_ok(self):
        """Test GET map response is OK."""
        conn = connect_DB()
        db_cursor = conn.cursor()
        # Insert a row of data
        db_cursor.execute("insert into maps values (?, ?, ?)", ('Done', '', ''))
        # Save (commit) the changes
        created_id = db_cursor.lastrowid
        conn.commit()
        result = self.simulate_get('/map/{0}'.format(created_id))
        assert(result.status == falcon.HTTP_200)
        conn.close()

    def test_map_get_gone(self):
        """Test GET map response is OK."""
        conn = connect_DB()
        db_cursor = conn.cursor()
        # Insert a row of data
        db_cursor.execute("insert into maps values (?, ?, ?)", ('Done', '', ''))
        # Save (commit) the changes
        created_id = db_cursor.lastrowid
        conn.commit()
        conn.close()
        self.simulate_delete('/map/{0}'.format(created_id))
        result = self.simulate_get('/map/{0}'.format(created_id))
        assert(result.status == falcon.HTTP_410)

    def test_map_get_delete(self):
        """Test DELETE map response is OK."""
        conn = connect_DB()
        db_cursor = conn.cursor()
        # Insert a row of data
        db_cursor.execute("insert into maps values (?, ?, ?)", ('Done', '', ''))
        # Save (commit) the changes
        created_id = db_cursor.lastrowid
        conn.commit()
        result = self.simulate_delete('/map/{0}'.format(created_id))
        assert(result.status == falcon.HTTP_200)
        conn.close()


if __name__ == "__main__":
    unittest.main()
