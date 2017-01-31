import falcon
import json
# from random import randint, choice
from gm_api.utils.validate import validate
from gm_api.resources import load_schema
from gm_api.utils.logs import main_logger
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
        resp.data = json.dumps(response)
        resp.content_type = 'application/json'
        resp.status = falcon.HTTP_202
        main_logger.info('Creating/POST a new map with ID: {0}.'.format(response['id']))


class MappingResource(object):
    """Create Mapping Resource based on ID for retrieval."""

    @validate(load_schema('mapids'))
    def on_get(self, req, resp, mapID):
        """Respond on GET request to map endpoint."""
        data = MappingObject()
        response = data.retrieve_mapID(mapID)
        if response is not None:
            resp.data = json.dumps(response)
            resp.content_type = 'application/json'
            resp.status = falcon.HTTP_200
        else:
            raise falcon.HTTPGone()
        main_logger.info('GET the map with ID: {0}.'.format(mapID))

    # @validate(load_schema('mapids'))
    def on_delete(self, req, resp, mapID):
        """Respond on GET request to map endpoint."""
        data = MappingObject()
        data.delete_mapID(mapID)
        resp.data = json.dumps({"deletedID": mapID})
        resp.content_type = 'application/json'
        main_logger.info('Delete/DELETE a map with ID: {0}.'.format(mapID))
        resp.status = falcon.HTTP_200
