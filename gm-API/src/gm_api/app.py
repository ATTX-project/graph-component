import falcon
from gm_api.api.linking import LinkClass, LinkingResource, RetrieveStrategies, StrategyResource
from gm_api.api.cluster import Cluster
from gm_api.api.healthcheck import HealthCheck
from gm_api.utils.logs import main_logger
from gm_api.api.provenance import Provenance
from gm_api.api.indexing import IndexClass, IndexingResource
from gm_api.api.graph_endpoint import GraphStatistics, GraphList, GraphResource, GraphSPARQL, GraphUpdate

api_version = "0.1"  # TO DO: Figure out a better way to do versioning


def init_api():
    """Create the API endpoint."""
    gm_app = falcon.API()

    gm_app.add_route('/health', HealthCheck())
    gm_app.add_route('/%s/index' % (api_version), IndexClass())
    gm_app.add_route('/%s/index/{indexID}' % (api_version), IndexingResource())
    gm_app.add_route('/%s/cluster' % (api_version), Cluster())
    gm_app.add_route('/%s/link' % (api_version), LinkClass())
    gm_app.add_route('/%s/link/{linkID}' % (api_version), LinkingResource())
    gm_app.add_route('/%s/linkstrategy' % (api_version), RetrieveStrategies())
    gm_app.add_route('/%s/linkstrategy/{strategyID}' % (api_version), StrategyResource())
    gm_app.add_route('/%s/prov' % (api_version), Provenance())

    gm_app.add_route('/%s/graph/query' % (api_version), GraphSPARQL())
    gm_app.add_route('/%s/graph/update' % (api_version), GraphUpdate())
    gm_app.add_route('/%s/graph/list' % (api_version), GraphList())
    gm_app.add_route('/%s/graph/statistics' % (api_version), GraphStatistics())
    gm_app.add_route('/%s/graph' % (api_version), GraphResource())

    main_logger.info('GM API is running.')
    return gm_app


if __name__ == '__main__':
    init_api()
