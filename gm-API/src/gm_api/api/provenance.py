import json
import falcon
from gm_api.utils.logs import app_logger
from gm_api.applib.update_prov import UpdateProv


class Provenance(object):
    """Update Provenance on request."""

    def on_get(self, req, resp):
        """Respond on GET request to map endpoint."""
        modifiedSince = req.get_param('modifiedSince')
        start = req.get_param_as_bool('start')
        wf_host = default_endpoint(req.get_param('wfapi'))
        graph_host = default_endpoint(req.get_param('graphStore'))
        data = UpdateProv()
        graph_store = {'host': graph_host, 'port': 3030, 'dataset': 'ds'}
        wf_endpoint = {'host': wf_host, 'port': 4301, 'version': "0.1"}
        prov_args = [graph_store, wf_endpoint, modifiedSince, start]
        result = data.do_update(*prov_args)
        resp.data = json.dumps(result)
        resp.content_type = 'application/json'
        resp.status = falcon.HTTP_200
        app_logger.info('Finished operations on /prov GET Request.')


def default_endpoint(param):
    """Establish default endpoint."""
    if param is not None:
        return param
    else:
        return 'localhost'
