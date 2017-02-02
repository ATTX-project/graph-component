from rdflib import ConjunctiveGraph, URIRef
import itertools
from rdflib.store import NO_STORE, VALID_STORE
import falcon
import requests
import tempfile
import random
from gm_api.utils.logs import app_logger

# static properties for now. move to ontology later.
properties = [
    'http://purl.org/dc/terms/identifier',
    'http://data.hulib.helsinki.fi/attx/urn',
    'http://purl.org/dc/terms/identifier'
]

# TEMP solution
file1 = tempfile.NamedTemporaryFile(dir='.', delete=False)
path = str(file1.name).rsplit('/', 1)[-1] + str(random.getrandbits(128))


class ClusterIDs(object):
    """Cluster IDs in the Graph Store based on the working graphs."""

    @classmethod
    def cluster(cls, endpoint=None, graph_namespace=None):
        """Cluster the IDs into the IDs Graph."""
        graph = ConjunctiveGraph(store="SPARQLStore")
        graph.open("http://{0}:{1}/{2}/query".format(endpoint['host'], endpoint['port'], endpoint['dataset']))

        storage = ConjunctiveGraph('Sleepycat')
        rt = storage.open(path, create=False)

        if rt == NO_STORE:
            # There is no underlying Sleepycat infrastructure, create it
            storage.open(path, create=True)
        else:
            assert rt == VALID_STORE, 'The underlying store is corrupt'
        try:
            datasets = cls.retrieve_workingGraphs(graph_namespace, endpoint)
            zip_list = list(itertools.product(datasets, properties))
            for i, j in zip_list:
                for s, p, o in graph.triples((None, URIRef('{0}'.format(j)), None), URIRef('{0}'.format(i))):
                    storage.add((s, URIRef('http://data.hulib.helsinki.fi/attx/id'), o))
            # if the 200 does not happen something is wrong
            response = cls.update_id_graph(endpoint, graph_namespace)
            if response == 200:
                pass
            else:
                app_logger.info('Something is wrong with updateing the graph of IDs. Status is {0}'.format(cls.update_id_graph(endpoint, graph_namespace)))
            app_logger.info('Clustered and added exactly: {0} triples to the graph.'.format(len(storage)))
            return {"status": "Processed.", "IDCount": len(storage)}
        except Exception as error:
            app_logger.error('Something is wrong: {0}'.format(error))
            raise falcon.HTTPUnprocessableEntity(
                'Unprocessable ID Clustering',
                'Could not process the clustering of ids.'
            )
        finally:
            storage.remove((None, None, None))
            app_logger.info('Cleaned local graph.')
            storage.close()

    @staticmethod
    def update_id_graph(endpoint, context=None):
        """Post data to a target Graph Store."""
        try:
            graph = None

            # reopen the graph
            graph = ConjunctiveGraph('Sleepycat')
            graph.open(path, create=False)
            if context is None:
                store_api = "http://{0}:{1}/{2}/data".format(endpoint['host'], endpoint['port'], endpoint['dataset'])
            else:
                store_api = "http://{0}:{1}/{2}/data?graph={3}ids".format(endpoint['host'], endpoint['port'], endpoint['dataset'], context)
            headers = {'Content-Type': 'application/trig'}
            result = requests.post(store_api, data=graph.serialize(format='trig'), headers=headers)
            app_logger.info('Add to graph: "{0}" the new IDs.'.format(context))
            return result.status_code
        except Exception as error:
            app_logger.error('Something is wrong: {0}'.format(error))
            raise falcon.HTTPUnprocessableEntity(
                'Unprocessable ID Clustering',
                'Could post the IDs to the graph store.'
            )
        finally:
            graph.close()

    @staticmethod
    def retrieve_workingGraphs(namespace, endpoint):
        """Retrieve Working Graph based on Workflow Execution information."""
        datasets = []
        query = """SELECT DISTINCT ?dataset
                   WHERE { GRAPH <%sprov> {
                   ?dataset a kaisa:Dataset.
                   ?activity prov:used ?dataset.
                   }}""" % (namespace)
        graph = ConjunctiveGraph(store="SPARQLStore")

        graph.open("http://{0}:{1}/ds/query".format(endpoint['host'], endpoint['port']))
        for row in graph.query(query, initNs={"kaisa": URIRef(namespace), "prov": URIRef('http://www.w3.org/ns/prov#')}):
            datasets.append(row[0].toPython())
        app_logger.info('Retrieve the working graphs for the ID clustering. Number of graphs: {0}.'.format(len(datasets)))
        return datasets
