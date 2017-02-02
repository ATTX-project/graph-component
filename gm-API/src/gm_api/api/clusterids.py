import falcon
import json
from gm_api.applib.construct_ids import ClusterIDs
from gm_api.utils.logs import app_logger


class Cluster(object):
    """Create Mapping class."""

    def on_post(self, req, resp):
        """Respond on GET request to map endpoint."""
        data = ClusterIDs()
        graph_namespace = 'http://data.hulib.helsinki.fi/attx/'
        endpoint = {'host': 'localhost', 'port': 3030, 'dataset': 'ds'}
        reponse = data.cluster(endpoint, graph_namespace)
        resp.data = json.dumps(reponse, indent=1, sort_keys=True)
        resp.content_type = 'application/json'
        resp.status = falcon.HTTP_200  # implement 202 when it is needed
        app_logger.info('Clustering IDs.')
