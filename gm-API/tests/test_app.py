from gm_api.app import create
import unittest
from falcon import testing


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

    def test_get_map_message(self):
        """Test GET map message."""
        # doc = {u'id': 4, u'status': u'WIP'}
        # #
        # result = self.simulate_get('/map/4')
        # # print dir(result)
        # self.assertEqual(result.json, doc)
        pass


if __name__ == "__main__":
    unittest.main()
