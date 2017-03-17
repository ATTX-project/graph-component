import re
import falcon
from gm_api.utils.logs import app_logger
from rdflib import ConjunctiveGraph, RDF
from gm_api.utils.prefixes import ATTXOnto, ATTXStrategy, ATTXBase


def retrieve_strategies(endpoint):
    """Retrieve list of strategies from the Graph Store."""
    datasets = []
    try:
        # TO DO: Does rdflib have an easier way for distinct ?
        query = """SELECT DISTINCT ?subject
                   WHERE { GRAPH <%s> {
                   ?subject a attxonto:LinkStrategy.
                   }}""" % (ATTXStrategy)
        graph = ConjunctiveGraph(store="SPARQLStore")

        graph.open("http://{0}:{1}/{2}/query".format(endpoint['host'], endpoint['port'], endpoint['dataset']))
        for row in graph.query(query, initNs={"attxonto": ATTXOnto}):
            datasets.append({"uri": row[0].toPython()})
        if len(datasets) > 0:
            app_logger.info('Retrieve list of strategies with their URIs. Number of strategies: {0}.'.format(len(datasets)))
            return datasets
        else:
            app_logger.info('Nothing to retrieve. No strategis are loaded in the graph.')
            return None
    except Exception as error:
        app_logger.error('Something is wrong: {0}'.format(error))
        raise falcon.HTTPUnprocessableEntity(
            'Unprocessable Linking Strategies',
            'Could not get the strategies list.'
        )
        return error
    finally:
        graph.close()


def retrieve_strategy(endpoint, strategyID):
    """Retrieve a specific strategy from the Graph Store and associated parameters."""
    parameters = {}
    try:
        # TO DO: Does rdflib have an easier way for distinct ?
        query = """SELECT ?property ?value
                   WHERE { GRAPH <%s> {
                   <%s%s> ?property ?value .
                   filter ( ?property not in ( rdf:type ) )
                   }}""" % (ATTXStrategy, ATTXBase, strategyID)
        graph = ConjunctiveGraph(store="SPARQLStore")

        graph.open("http://{0}:{1}/{2}/query".format(endpoint['host'], endpoint['port'], endpoint['dataset']))
        for row in graph.query(query, initNs={"attxonto": ATTXOnto, "rdf": RDF}):
            parameters[re.split(r'\/|\#', row[0].toPython())[-1]] = row[1].toPython()
        if len(parameters) > 0:
            strategy = {"uri": ATTXBase + strategyID, "parameters": parameters}
            app_logger.info('Retrieve parameters for the strategy with ID: {0}{1}.'.format(ATTXBase, strategyID))
            return strategy
        else:
            app_logger.info('Nothing to retrieve. Strategy does not exist')
            return None
    except Exception as error:
        app_logger.error('Something is wrong: {0}'.format(error))
        raise falcon.HTTPUnprocessableEntity(
            'Unprocessable Linking Strategy',
            'Could not get a specific strategy.'
        )
        return error
    finally:
        graph.close()
