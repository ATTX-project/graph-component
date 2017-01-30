from gm_api.app import gm_app
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
        self.app = gm_app.create()


class TestMyApp(appTest):
    """Testing GM app and initialize it for that purpose."""

    def test_get_map_message(self):
        """Test GET map message."""
        # doc = {u'message': u'Hello world!'}

        result = self.simulate_get('/map/42')
        print dir(result)
        # self.assertEqual(result.json, doc)


if __name__ == "__main__":
    unittest.main()
