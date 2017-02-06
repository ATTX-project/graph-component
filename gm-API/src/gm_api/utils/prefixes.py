from ConfigParser import SafeConfigParser


def bind_prefix(graph):
    """Bind Prefixes fro graph."""
    graph.bind('kaisa', 'http://data.hulib.helsinki.fi/attx/')
    graph.bind('dc', 'http://purl.org/dc/elements/1.1/')
    graph.bind('schema', 'http://schema.org/')
    graph.bind('pwo', 'http://purl.org/spar/pwo/')
    graph.bind('prov', 'http://www.w3.org/ns/prov#')
    graph.bind('dcterms', 'http://purl.org/dc/terms/')
    graph.bind('sd', 'http://www.w3.org/ns/sparql-service-description#')

    return graph


# TBD
def namspace_config(config_file):
    """Read Namespace config from file."""
    parser = SafeConfigParser()
    parser.read(config_file)

    pass
