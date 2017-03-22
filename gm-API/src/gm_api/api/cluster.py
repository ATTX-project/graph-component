import json
import falcon
from gm_api.schemas import load_schema
from gm_api.utils.logs import app_logger
from gm_api.utils.validate import validate
from gm_api.applib.construct_cluster import ClusterID


class Cluster(object):
    """Create Cluster of IDs class."""

    @validate(load_schema('cluster'))
    def on_post(self, req, resp, parsed):
        """Respond on GET request to map endpoint."""
        data = ClusterID()
        endpoint = parsed.get('graphStore')
        graph_store = {'host': endpoint['host'], 'port': endpoint['port'], 'dataset': endpoint['dataset']}
        reponse = data.cluster(graph_store)
        resp.data = json.dumps(reponse, indent=1, sort_keys=True)
        resp.content_type = 'application/json'
        resp.status = falcon.HTTP_200  # implement 202 when it is needed
        app_logger.info('Finished operations on /cluster POST Request.')
