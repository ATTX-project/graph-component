import falcon
from gm_api.api.linking import Link
from gm_api.api.cluster import Cluster
from gm_api.utils.logs import main_logger
from gm_api.api.provenance import Provenance
from gm_api.api.indexing import IndexClass, IndexingResource

api_version = "0.1"  # TO DO: Figure out a better way to do versioning


def create():
    """Create the API endpoint."""
    do_index = IndexClass()
    do_cluster = Cluster()
    do_link = Link()
    get_prov = Provenance()

    get_index = IndexingResource()

    gm_app = falcon.API()
    gm_app.add_route('/%s/index' % (api_version), do_index)
    gm_app.add_route('/%s/index/{indexID}' % (api_version), get_index)
    gm_app.add_route('/%s/cluster' % (api_version), do_cluster)
    gm_app.add_route('/%s/link' % (api_version), do_link)
    gm_app.add_route('/%s/prov' % (api_version), get_prov)
    main_logger.info('App is running.')
    return gm_app


if __name__ == '__main__':
    create()
