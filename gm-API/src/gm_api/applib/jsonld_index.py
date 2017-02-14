import json
import subprocess
from pyld import jsonld
from subprocess import STDOUT, PIPE
from rdflib import ConjunctiveGraph
from elasticsearch import Elasticsearch
from SPARQLWrapper import SPARQLWrapper
from gm_api.utils.prefixes import bind_prefix
from gm_api.utils.logs import app_logger


class LODResource(object):
    """Create and Index Linked Data Resource."""

    @classmethod
    def map_jsonld(cls, targetEndpoint, graphStore, query, context, resource_type, mapID, index=None):
        """Perform the mapping and index to Elasticsearch."""
        graph = cls.create_graph(graphStore, query)
        resource = cls.create_jsonLD(graph, context)
        cls.index_resource(targetEndpoint, resource, mapID, resource_type)
        app_logger.info('Performed PYTHON based mapping from "{0}" and indexing at "{1}".'.format(graphStore['endpoint']['host'], targetEndpoint['host']))

    @classmethod
    def map_esJava(cls, java_file, graphStore, targetEndpoint, mapping):
        """Return ES parameters for elasticsearch 1.3.4 to run process."""
        storeEndpoint = 'http://{0}:{1}/{2}/'.format(graphStore['endpoint']['host'], graphStore['endpoint']['port'], graphStore['endpoint']['dataset'])

        graphs = ','.join(graphStore['graphs'])
        port = str(targetEndpoint['port'])
        index = str(targetEndpoint['host'])
        mapping = mapping['query']
        cmd = ['java', '-jar', java_file, '-b', '1', '-i', index, '-p', port, '-s', storeEndpoint, '-m', mapping, '-g', graphs]
        process = subprocess.Popen(cmd, stdout=PIPE, stderr=STDOUT)
        for line in iter(process.stdout.readline, ''):
            app_logger.info('[JAVA RUN log {0}]: {1}'.format(process.pid, line))
        process.terminate()
        app_logger.info('Performed JAVA based mapping from "{0}" and indexing at "{1}".'.format(graphStore['endpoint']['host'], targetEndpoint['host']))

    @staticmethod
    def index_resource(targetEndpoint, graph, mapID, resource_type, index=None):
        """Index JSON-LD graph at specific ES endpoint."""
        es = Elasticsearch([{'host': targetEndpoint['host'], 'port': targetEndpoint['port']}])
        if index is not None and not(es.indices.exists(index)):
            es.indices.create(index=index, ignore=400)
        else:
            index = 'default'
        es.index(index=index, doc_type=resource_type, id=mapID, body=json.loads(graph))
        app_logger.info("Index in Elasticsearch at index: \"{0}\" with type: \"{1}\" and ID: \"{2}\".".format(index, resource_type, mapID))
        return

    @staticmethod
    def create_graph(graphStore, query):
        """Retrieve graph based on mapping."""
        graph = ConjunctiveGraph()
        bind_prefix(graph)
        store = graphStore['endpoint']
        store_api = "http://{0}:{1}/{2}/query".format(store['host'], store['port'], store['dataset'])
        try:
            sparql = SPARQLWrapper(store_api)
            # add a default graph, though that can also be in the query string
            for named_graph in graphStore['graphs']:
                sparql.addDefaultGraph(named_graph)
            sparql.setQuery(query)
            app_logger.info('Construct outputgraph based on endpoint.')
            data = sparql.query().convert()
            graph.parse(data=data.serialize(), format='xml')
            return graph
        except Exception as error:
            app_logger.error('Mapping Failed!')
            return error

    @staticmethod
    def create_jsonLD(graph, graph_context):
        """Create JSON-LD output for the given subject."""
        try:
            # pyld likes nquads, by default
            expand = jsonld.from_rdf(graph.serialize(format="nquads"))
            # framed = jsonld.frame(doc, json.load(open('example_frame.jsonld', 'r')))
            # compact a document according to a particular context
            # see: http://json-ld.org/spec/latest/json-ld/#compacted-document-form
            context = graph_context
            compacted = jsonld.compact(expand, context)
            # result = json.dumps(jsonld.flatten(expand), indent=1, sort_keys=True)
            # normalized = jsonld.normalize(expand, {'algorithm': 'URDNA2015', 'format': 'application/json+ld'})
            result = json.dumps(compacted, indent=1, sort_keys=True)
            app_logger.info('Serialized as JSON-LD compact with the context: {0}'.format(context))
            return result
        except Exception as error:
            app_logger.error('Mapping Failed!')
            return error
