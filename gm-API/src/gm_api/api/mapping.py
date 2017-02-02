import falcon
import json
# from random import randint, choice
from gm_api.utils.validate import validate
from gm_api.resources import load_schema
from gm_api.utils.logs import app_logger
from gm_api.lib.construct_map import MappingObject


class Mapping(object):
    """Create Mapping class."""

    @validate(load_schema('map'))
    def on_post(self, req, resp, parsed):
        """Respond on GET request to map endpoint."""
        data = MappingObject()
        response = data.create_map(
            parsed.get('targetEndpoint'),
            parsed.get('mapping'),
            parsed.get('sourceGraphs'),
            parsed.get('plugin'),
            parsed.get('format'))
        resp.data = json.dumps(response, indent=1, sort_keys=True)
        resp.content_type = 'application/json'
        resp.status = falcon.HTTP_202
        app_logger.info('Creating/POST a new map with ID: {0}.'.format(response['id']))


class MappingResource(object):
    """Create Mapping Resource based on ID for retrieval."""

    @validate(load_schema('mapids'))
    def on_get(self, req, resp, mapID):
        """Respond on GET request to map endpoint."""
        data = MappingObject()
        response = data.retrieve_mapID(mapID)
        if response is not None:
            resp.data = json.dumps(response, indent=1, sort_keys=True)
            resp.content_type = 'application/json'
            resp.status = falcon.HTTP_200
            app_logger.info('GET the map with ID: {0}.'.format(mapID))
        else:
            raise falcon.HTTPGone()
            app_logger.warning('Map with ID: {0} is gone.'.format(mapID))

    def on_delete(self, req, resp, mapID):
        """Respond on GET request to map endpoint."""
        data = MappingObject()
        data.delete_mapID(mapID)
        resp.data = json.dumps({"deletedID": mapID}, indent=1, sort_keys=True)
        resp.content_type = 'application/json'
        app_logger.info('Deleted/DELETE a map with ID: {0}.'.format(mapID))
        resp.status = falcon.HTTP_200
