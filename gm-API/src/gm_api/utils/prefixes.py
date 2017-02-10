from rdflib import Namespace
from ConfigParser import SafeConfigParser

PROV = Namespace('http://www.w3.org/ns/prov#')
ATTXURL = 'http://data.hulib.helsinki.fi/attx/'
ATTXBase = Namespace('http://data.hulib.helsinki.fi/attx/')
ATTXIDs = Namespace('http://data.hulib.helsinki.fi/attx/ids')
ATTXProv = Namespace('http://data.hulib.helsinki.fi/attx/prov')
ATTXOnto = Namespace('http://data.hulib.helsinki.fi/attx/onto#')
SD = Namespace('http://www.w3.org/ns/sparql-service-description#')


def bind_prefix(graph):
    """Bind Prefixes fro graph."""
    graph.bind('schema', 'http://schema.org/')
    graph.bind('pwo', 'http://purl.org/spar/pwo/')
    graph.bind('prov', 'http://www.w3.org/ns/prov#')
    graph.bind('dcterms', 'http://purl.org/dc/terms/')
    graph.bind('dc', 'http://purl.org/dc/elements/1.1/')
    graph.bind('attx', 'http://data.hulib.helsinki.fi/attx/')
    graph.bind('attxonto', 'http://data.hulib.helsinki.fi/attx/onto#')
    graph.bind('sd', 'http://www.w3.org/ns/sparql-service-description#')

    return graph


# TBD
def namspace_config(config_file):
    """Read Namespace config from file."""
    parser = SafeConfigParser()
    parser.read(config_file)

    pass
