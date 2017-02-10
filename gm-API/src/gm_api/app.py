import falcon
from gm_api.api.linking import Link
from gm_api.api.cluster import Cluster
from gm_api.utils.logs import main_logger
from gm_api.api.provenance import Provenance
from gm_api.api.mapping import Mapping, MappingResource

api_version = "0.1"  # TO DO: Figure out a better way to do versioning


def create():
    """Create the API endpoint."""
    do_map = Mapping()
    do_cluster = Cluster()
    do_link = Link()
    get_prov = Provenance()

    get_map = MappingResource()

    gm_app = falcon.API()
    gm_app.add_route('/%s/map' % (api_version), do_map)
    gm_app.add_route('/%s/map/{mapID}' % (api_version), get_map)
    gm_app.add_route('/%s/cluster' % (api_version), do_cluster)
    gm_app.add_route('/%s/link' % (api_version), do_link)
    gm_app.add_route('/%s/prov' % (api_version), get_prov)
    main_logger.info('App is running.')
    return gm_app


if __name__ == '__main__':
    create()
