import json
import falcon
from gm_api.utils.logs import app_logger
from gm_api.schemas import load_schema
from gm_api.utils.validate import validate
from gm_api.applib.construct_index import IndexingObject


class IndexClass(object):
    """Create Indexing class for API."""

    @validate(load_schema('map'))
    def on_post(self, req, resp, parsed):
        """Respond on GET request to index endpoint."""
        data = IndexingObject()
        index_args = [parsed.get('targetEndpoint'), parsed.get('indexing'), parsed.get('graphStore'), parsed.get('plugin')]
        response = data.create_index(*index_args)
        resp.data = json.dumps(response, indent=1, sort_keys=True)
        resp.content_type = 'application/json'
        resp.status = falcon.HTTP_202
        app_logger.info('Creating/POST a new index with ID: {0}.'.format(response['id']))


class IndexingResource(object):
    """Create Indexing Resource based on ID for retrieval."""

    @validate(load_schema('mapids'))
    def on_get(self, req, resp, indexID):
        """Respond on GET request to index endpoint."""
        data = IndexingObject()
        response = data.retrieve_indexID(indexID)
        if response is not None:
            resp.data = json.dumps(response, indent=1, sort_keys=True)
            resp.content_type = 'application/json'
            resp.status = falcon.HTTP_200
            app_logger.info('GET the index with ID: {0}.'.format(indexID))
        else:
            raise falcon.HTTPGone()
            app_logger.warning('Index with ID: {0} is gone.'.format(indexID))

    def on_delete(self, req, resp, indexID):
        """Respond on GET request to index endpoint."""
        data = IndexingObject()
        data.delete_indexID(indexID)
        resp.data = json.dumps({"deletedID": indexID}, indent=1, sort_keys=True)
        resp.content_type = 'application/json'
        app_logger.info('Deleted/DELETE a index with ID: {0}.'.format(indexID))
        resp.status = falcon.HTTP_200
