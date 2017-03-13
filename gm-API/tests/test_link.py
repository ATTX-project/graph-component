# import falcon
import unittest
from falcon import testing
from gm_api.app import create
# from gm_api.app import api_version
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

    # def test_getlinks_post_good(self):
    #     """Test good POST entities links message."""
    #     with open('tests/resources/links_request.json', 'r') as datafile:
    #         test_data = datafile.read().replace('\n', '')
    #     hdrs = [('Accept', 'application/json'),
    #             ('Content-Type', 'application/json'), ]
    #     result = self.simulate_post('/%s/link' % (api_version), body=test_data, headers=hdrs)
    #     assert(result.status == falcon.HTTP_202)
    #
    # def test_getlinks_post_bad(self):
    #     """Test empty POST links message."""
    #     hdrs = [('Accept', 'application/json'),
    #             ('Content-Type', 'application/json'), ]
    #     result = self.simulate_post('/%s/link' % (api_version), body='', headers=hdrs)
    #     assert(result.status == falcon.HTTP_400)
    #
    # def test_getlinks_get_good(self):
    #     """Test GET entities links message."""
    #     result = self.simulate_get('/%s/link' % (api_version))
    #     assert(result.status == falcon.HTTP_200)


if __name__ == "__main__":
    unittest.main()
