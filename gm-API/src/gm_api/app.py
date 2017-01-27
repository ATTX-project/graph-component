import falcon
from gm_api.api.mapping import Mapping, MappingResource
from wsgiref import simple_server  # NOQA
from gm_api.utils.logs import main_logger

sample_resource = Mapping()
resource_response = MappingResource()

gm_app = falcon.API()
gm_app.add_route('/map', sample_resource)
gm_app.add_route('/map/{mapID}', resource_response)
gm_app.add_route('/clusterids', sample_resource)
gm_app.add_route('/links', sample_resource)


if __name__ == '__main__':
    httpd = simple_server.make_server('0.0.0.0', 4302, gm_app)
    httpd.serve_forever()
    main_logger.info('App is running.')
