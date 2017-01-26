import falcon
import json


class Mapping(object):
    """Create Mapping class."""

    def on_get(self, req, resp):
        """Respond on GET request to map endpoint."""
        resp.data = json.dumps({'message': 'Hello world!'})
        resp.content_type = 'application/json'
        resp.status = falcon.HTTP_200

    def on_delete(self, req, resp):
        """Respond on GET request to map endpoint."""
        resp.data = json.dumps({'message': 'Hello world!'})
        resp.content_type = 'application/json'
        resp.status = falcon.HTTP_200

    def on_post(self, req, resp):
        """Respond on GET request to map endpoint."""
        resp.data = json.dumps({'message': 'Hello world!'})
        resp.content_type = 'application/json'
        resp.status = falcon.HTTP_200
