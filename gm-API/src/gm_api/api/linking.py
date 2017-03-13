import json
import falcon
from gm_api.utils.validate import validate
from gm_api.schemas import load_schema
from gm_api.utils.logs import app_logger
from gm_api.applib.construct_links import LinkingObject
from gm_api.applib.retrieve_linkstrategy import StrategyObject


class LinkClass(object):
    """Create Mapping class."""

    @validate(load_schema('links'))
    def on_post(self, req, resp, parsed):
        """Respond on POST request to link endpoint."""
        data = LinkingObject()
        index_args = [parsed.get('strategy'), parsed.get('graphStore')]
        response = data.create_links(*index_args)
        resp.data = json.dumps(response, indent=1, sort_keys=True)
        resp.content_type = 'application/json'
        resp.status = falcon.HTTP_202
        app_logger.info('Finished operations on /link POST Request.')


class LinkingResource(object):
    """Create Linking Resource based on ID for retrieval."""

    @validate(load_schema('idtype'))
    def on_get(self, req, resp, linkID):
        """Respond on GET request to link endpoint."""
        data = LinkingObject()
        response = data.retrieve_linkID(linkID)
        if response is not None:
            resp.data = json.dumps(response, indent=1, sort_keys=True)
            resp.content_type = 'application/json'
            resp.status = falcon.HTTP_200
            app_logger.info('GET /link the linking job with ID: {0}.'.format(linkID))
        else:
            raise falcon.HTTPGone()
            app_logger.warning('Index with ID: {0} is gone.'.format(linkID))

    def on_delete(self, req, resp, linkID):
        """Respond on GET request to index endpoint."""
        data = LinkingObject()
        data.delete_linkID(linkID)
        resp.data = json.dumps({"deletedID": linkID}, indent=1, sort_keys=True)
        resp.content_type = 'application/json'
        app_logger.info('Deleted/DELETE /link a linking job with ID: {0}.'.format(linkID))
        resp.status = falcon.HTTP_200


class StrategyResource(object):
    """Create Mapping class."""

    @validate(load_schema('idtype'))
    def on_get(self, req, resp, strategyID):
        """Respond on GET request to index endpoint."""
        data = StrategyObject()
        response = data.retrieve_linkID(strategyID)
        if response is not None:
            resp.data = json.dumps(response, indent=1, sort_keys=True)
            resp.content_type = 'application/json'
            resp.status = falcon.HTTP_200
            app_logger.info('GET /linkstrategy the index with ID: {0}.'.format(strategyID))
        else:
            raise falcon.HTTPGone()
            app_logger.warning('Index with ID: {0} is gone.'.format(strategyID))


class RetrieveStrategies(object):
    """Create Mapping class."""

    def on_get(self, req, resp):
        """Respond on GET request to index endpoint."""
        resp.data = json.dumps()
        resp.content_type = 'application/json'
        resp.status = falcon.HTTP_202
        app_logger.info('GET /linkstrategy linking strategies available in the Graph Store.')
