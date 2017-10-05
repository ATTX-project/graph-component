## Graph Component

The ATTX Graph component associated to this project has the main goal of aggregating the data that flows within the platform, types of transformations (and associated workflows), the provenance information (agent and ETL processes performed) and other metadata.

The Graph Component consists of following parts:
- Graph Manager Service
- Graph Store
- Ontology/Data model

Graph Component is designed to:
* manage interaction with the graph store;
* Store provenance related information, store linking strategies and other working data in the Graph Store.

### Clone repository
```
    git clone --recursive https://github.com/ATTX-project/graph-component

    git submodule update --init --recursive
```