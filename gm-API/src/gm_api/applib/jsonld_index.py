import json
import subprocess
from pyld import jsonld
from subprocess import STDOUT, PIPE
from rdflib import ConjunctiveGraph
# from rdflib.namespace import RDF
# from rdflib.util import get_tree
from elasticsearch import Elasticsearch
# from SPARQLWrapper import SPARQLWrapper
from gm_api.utils.prefixes import bind_prefix
from gm_api.utils.logs import app_logger


class LODResource(object):
    """Create and Index Linked Data Resource."""

    @classmethod
    def index_json(cls, targetEndpoint, graphStore, indexing):
        """Perform the indexing and index to Elasticsearch 5."""
        # subjects = []
        # for subject in graph.subjects(RDF.type, None):
        #     print get_tree(graph, subject, None)
        indexingID = indexing["indexingID"]
        graph = cls.create_graph(graphStore)
        framedRDF = cls.create_jsonLD(graph, indexing['filter'])
        jdata = json.loads(framedRDF)
        for resource in jdata["@graph"]:
            cls.index_resource(targetEndpoint, json.dumps(resource), resource[indexingID], indexing['resourceType'], indexing['index'])
        app_logger.info('Performed PYTHON based indexing from "{0}" and indexing at "{1}".'.format(graphStore['endpoint']['host'], targetEndpoint['host']))

    @classmethod
    def index_esJava(cls, java_file, graphStore, targetEndpoint, indexing):
        """Return ES parameters for elasticsearch 1.3.4 to run process."""
        storeEndpoint = 'http://{0}:{1}/{2}/'.format(graphStore['endpoint']['host'], graphStore['endpoint']['port'], graphStore['endpoint']['dataset'])

        graphs = ','.join(graphStore['graphs'])
        port = str(targetEndpoint['port'])
        index = str(targetEndpoint['host'])
        indexing = indexing['filter']
        cmd = ['java', '-jar', java_file, '-b', '1', '-i', index, '-p', port, '-s', storeEndpoint, '-m', indexing, '-g', graphs]

        process = subprocess.Popen(cmd, stdout=PIPE, stderr=STDOUT)

        for line in iter(process.stdout.readline, ''):
            app_logger.info('[JAVA RUN log {0}]: {1}'.format(process.pid, line))
        process.terminate()
        app_logger.info('Performed JAVA based indexing from "{0}" and indexing at "{1}".'.format(graphStore['endpoint']['host'], targetEndpoint['host']))

    @staticmethod
    def index_resource(targetEndpoint, graph, indexingID, resource_type, index=None):
        """Index JSON-LD graph at specific ES endpoint."""
        es = Elasticsearch([{'host': targetEndpoint['host'], 'port': targetEndpoint['port']}])
        if index is not None and not(es.indices.exists(index)):
            es.indices.create(index=index, ignore=400)
        else:
            index = 'default'
        es.index(index=index, doc_type=resource_type, id=indexingID, body=json.loads(graph))
        app_logger.info("Index in Elasticsearch at index: \"{0}\" with type: \"{1}\" and ID: \"{2}\".".format(index, resource_type, indexingID))
        return

    @staticmethod
    def create_graph(graphStore):
        """Retrieve graph based on indexing."""
        graph = ConjunctiveGraph()
        bind_prefix(graph)
        store = graphStore['endpoint']
        store_api = "http://{0}:{1}/{2}/data?graph=".format(store['host'], store['port'], store['dataset'])
        try:
            # sparql = SPARQLWrapper(store_api)
            # # add a default graph, though that can also be in the query string
            # for named_graph in graphStore['graphs']:
            #     sparql.addDefaultGraph(named_graph)
            # sparql.setQuery(query)
            for named_graph in graphStore['graphs']:
                graph.parse('{0}{1}'.format(store_api, named_graph))
            app_logger.info('Construct outputgraph based on endpoint.')
            # data = sparql.query().convert()
            # graph.parse(data=data.serialize(), format='xml')
            return graph
        except Exception as error:
            app_logger.error('Indexing failed when processing the graph! with error: {0}'.format(error))
            return error

    @staticmethod
    def create_jsonLD(graph, filter_frame):
        """Create JSON-LD output for the given subject."""
        try:
            # pyld likes nquads, by default
            expanded = jsonld.from_rdf(graph.serialize(format="nquads"))
            framed = jsonld.frame(expanded, json.loads(filter_frame))
            # compact a document according to a particular context
            # see: http://json-ld.org/spec/latest/json-ld/#compacted-document-form
            # context = graph_context
            # compacted = jsonld.compact(expand, filter_frame)
            result = json.dumps(framed, indent=1, sort_keys=True)
            app_logger.info('Serialized as JSON-LD compact with the frame: {0}'.format(filter_frame))
            return result
        except Exception as error:
            app_logger.error('Indexing failed when creating the JSON-LD! with error: {0}'.format(error))
            return error
