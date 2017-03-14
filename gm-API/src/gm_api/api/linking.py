import json
import falcon
from gm_api.utils.validate import validate
from gm_api.schemas import load_schema
from gm_api.utils.logs import app_logger
from gm_api.applib.construct_links import LinkingObject
from gm_api.applib.retrieve_linkstrategy import retrieve_strategy, retrieve_strategies


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
    """Retrieve specific strategy and its parameters from the Graph Store."""

    @validate(load_schema('idtype'))
    def on_get(self, req, resp, strategyID):
        """Respond on GET request to index endpoint."""
        graph_store = {'host': 'localhost', 'port': 3030, 'dataset': 'ds'}
        response = retrieve_strategy(graph_store, strategyID)
        if response is not None:
            resp.data = json.dumps(response, indent=1, sort_keys=True)
            resp.content_type = 'application/json'
            resp.status = falcon.HTTP_200
            app_logger.info('GET /linkstrategy for the strategy with ID: {0}.'.format(strategyID))
        else:
            raise falcon.HTTPNotFound()
            app_logger.warning('Linking Strategy with ID: {0} does not exist.'.format(strategyID))


class RetrieveStrategies(object):
    """Linking Strategies retrieval from the Graph Store."""

    def on_get(self, req, resp):
        """Respond on GET request to index endpoint."""
        graph_store = {'host': 'localhost', 'port': 3030, 'dataset': 'ds'}
        response = retrieve_strategies(graph_store)
        if response is not None:
            resp.data = json.dumps(response, indent=1, sort_keys=True)
            resp.content_type = 'application/json'
            resp.status = falcon.HTTP_200
            app_logger.info('GET /linkstrategy linking strategies available in the Graph Store.')
        else:
            raise falcon.HTTPNotFound()
            app_logger.warning('Did not find any Linking Strategies in the Graph Store')
