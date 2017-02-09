import json
import falcon
from gm_api.utils.logs import app_logger
from gm_api.applib.construct_cluster import ClusterID


class Cluster(object):
    """Create Mapping class."""

# TO DO: Add validation
    def on_post(self, req, resp):
        """Respond on GET request to map endpoint."""
        data = ClusterID()
        # TO DO: these settings need to come from either POST body or WF-API
        endpoint = {'host': 'localhost', 'port': 3030, 'dataset': 'ds'}
        reponse = data.cluster(endpoint)
        resp.data = json.dumps(reponse, indent=1, sort_keys=True)
        resp.content_type = 'application/json'
        resp.status = falcon.HTTP_200  # implement 202 when it is needed
        app_logger.info('Finished operations on /cluster POST Request.')
