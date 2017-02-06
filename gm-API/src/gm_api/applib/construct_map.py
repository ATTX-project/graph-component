from rdflib import ConjunctiveGraph
from SPARQLWrapper import SPARQLWrapper
from gm_api.utils.logs import app_logger, thread_logger
from gm_api.utils.db import connect_DB
from gm_api.utils.prefixes import bind_prefix
from elasticsearch import Elasticsearch
from pyld import jsonld
import json
import subprocess
import threading


class LODResource(object):
    """Create and Index Linked Data Resource."""

    @classmethod
    def map_lod(cls, targetEndpoint, sourceGraphs, query, context, resource_type, mapID, index=None, serialization=None):
        """Perform the mapping and index to Elasticsearch."""
        graph = cls.create_graph(sourceGraphs, query, context, serialization)
        cls.index_resource(targetEndpoint, graph, mapID, resource_type)
        app_logger.info('Performed mapping from "{0}" and indexing at "{1}".'.format(sourceGraphs['endpoint']['host'], targetEndpoint['host']))

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
    def create_graph(sourceGraphs, query, graph_context, serialization=None):
        """Index JSON-LD graph at specific endpoint."""
        graph = ConjunctiveGraph()
        bind_prefix(graph)
        store = sourceGraphs['endpoint']
        store_api = "http://{0}:{1}/{2}/query".format(store['host'], store['port'], store['dataset'])
        sparql = SPARQLWrapper(store_api)
        # add a default graph, though that can also be in the query string
        for named_graph in sourceGraphs['graphs']:
            sparql.addDefaultGraph(named_graph)
        sparql.setQuery(query)
        try:
            data = sparql.query().convert()
            graph.parse(data=data.serialize(), format='xml')
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


class MappingObject(object):
    """Construct Mapping based on plugin and mapping specification."""

    @classmethod
    def create_map(cls, targetEndpoint, mapping, sourceGraphs, plugin, serialization=None):
        """Create a map."""
        conn = connect_DB()
        result = cls.register_map(conn, mapping)
        d = threading.Thread(name='daemon', target=cls.daemon,
                             args=(result, targetEndpoint, mapping,
                                   sourceGraphs, plugin, serialization))
        d.setDaemon(True)
        d.start()
        app_logger.info('Construct map based on Map object.')
        return result

    @classmethod
    def retrieve_mapID(cls, mapID):
        """Retrieve map."""
        conn = connect_DB()
        result = cls.check_map_status(conn, mapID)
        app_logger.info('Retrieve map with the ID: {0}'.format(mapID))
        return result

    @classmethod
    def delete_mapID(cls, mapID):
        """Delete map."""
        conn = connect_DB()
        cls.delete_map(conn, mapID)
        app_logger.info('Delete map with the ID: {0}'.format(mapID))
        return

    @classmethod
    def daemon(cls, result, targetEndpoint, mapping, sourceGraphs, plugin, serialization):
        """Simple worker daemon."""
        conn = connect_DB()
        try:
            thread_logger.info('Starting Daemon thread.')
            if plugin == 'python':
                data = LODResource()
                data.map_lod(targetEndpoint, sourceGraphs, mapping['query'],
                             mapping['context'], mapping['resourceType'], result['id'],
                             mapping['index'], serialization)
                cls.update_map_status(conn, result['id'], "Done")
                # TO DO: have a return in order to update data
                # cls.update_map_data(conn, result['id'], data)
            elif plugin == 'java':
                conn = connect_DB()
                cls.update_map_status(conn, result['id'], "Done")
                subprocess.call(['java', '-jar', 'Blender.jar'])
            else:
                return
            thread_logger.info('Exiting thread!')
        except Exception as error:
            app_logger.error('Thread Failed!')
            cls.update_map_status(conn, result['id'], str(error))
            return error

    @staticmethod
    def register_map(conn, mapping):
        """Create the map."""
        db_cursor = conn.cursor()
        # Insert a row of data
        db_cursor.execute("insert into maps values (?, ?, ?)", ('WIP', '', str(mapping)))
        # Save (commit) the changes
        conn.commit()
        result = {'id': db_cursor.lastrowid, 'status': 'WIP'}
        app_logger.info('Create row in the database with ID: {0}'.format(db_cursor.lastrowid))
        db_cursor.close()
        return result

    @staticmethod
    def check_map_status(conn, mapID):
        """Check the map status."""
        db_cursor = conn.cursor()
        # Insert a row of data
        for row in db_cursor.execute('select rowid, status from maps where rowid=?', (mapID, )):
            result = {'id': row[0], 'status': row[1]}
            app_logger.info('Check status in the database for ID: {0}'.format(mapID))
            break
        else:
            result = None
            app_logger.warning('Check status: There is no record for ID: {0}'.format(mapID))
        db_cursor.close()
        return result

    @staticmethod
    def delete_map(conn, mapID):
        """Detele the map data."""
        db_cursor = conn.cursor()
        # Delete a row of data
        db_cursor.execute('delete from maps where rowid=?', (mapID, ))
        # Save (commit) the changes
        conn.commit()
        app_logger.info('Delete map in the database with ID: {0}'.format(mapID))
        db_cursor.close()
        return

    @staticmethod
    def update_map_status(conn, mapID, status):
        """Update the map status information."""
        db_cursor = conn.cursor()
        db_cursor.execute("SELECT rowid FROM maps WHERE rowid = ?", (mapID,))
        data = db_cursor.fetchone()
        if data is None:
            app_logger.info('There is no record for ID:'.format(mapID))
            pass
        else:
            db_cursor.execute('update maps set status=? where rowid=?', (status, mapID))
            # Save (commit) the changes
            conn.commit()
            app_logger.info('Update status in the database for ID: {0}'.format(mapID))
        db_cursor.close()
        return

    @staticmethod
    def update_map_data(conn, mapID, data):
        """Update the map data."""
        db_cursor = conn.cursor()
        db_cursor.execute("SELECT rowid FROM maps WHERE rowid = ?", (mapID,))
        data = db_cursor.fetchone()
        if data is None:
            app_logger.info('There is no record for ID:'.format(mapID))
            pass
        else:
            db_cursor.execute('update maps set data=? where rowid=?', (data, mapID, ))
            # Save (commit) the changes
            conn.commit()
            app_logger.info('Update data field in the database for ID: {0}'.format(mapID))
        db_cursor.close()
        return
