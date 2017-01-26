import falcon
from gc_api.api.mapping import Mapping
from wsgiref import simple_server  # NOQA
from gc_api.utils.logs import main_logger

sample_resource = Mapping()

gm_app = falcon.API()
gm_app.add_route('/map', sample_resource)
gm_app.add_route('/map/{mapID}', sample_resource)
gm_app.add_route('/clusterids', sample_resource)
gm_app.add_route('/links', sample_resource)


if __name__ == '__main__':
    httpd = simple_server.make_server('0.0.0.0', 4302, gm_app)
    httpd.serve_forever()
    main_logger.info('App is running.')
