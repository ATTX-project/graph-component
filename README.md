## Graph Component

The ATTX Graph component associated to this project has the main goal of aggregating the data that flows within the platform, types of transformations (and associated workflows), the provenance information (agent and ETL processes performed) and other metadata.

The Graph Component consists of following parts:
- Graph Manager API
- Graph Store

Graph Component is designed to:
* retrieve information from the Workflow API in order to update the **Provenance and Workflow Data** graphs;
* manage the mappings and indexing of data;
* source data queries - select specific data from the Graph Store and after applying the mapping deliver it to target output (e.g. Elasticsearch);
* Store provenance related information, store linking strategies and other working data in the Graph Store.
* Ontology based linking/clustering of IDs.

## Repository Structure

The repository consists of:
* RDF2JSON Elasticsearch indexer - used by the GM-API
* Graph Manager API
* Graph Component - Integration Tests
