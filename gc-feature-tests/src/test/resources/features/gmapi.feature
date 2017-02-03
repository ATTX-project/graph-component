Feature: Handle mapping and indexing from the graph to Elasticsearch
    Scenario: Add map and get its status
        Given graph API, Elasticsearch and Graph Store are running
        When I post a mapping
        Then I should be able to retrieve status of mapping.

    Scenario: Retrieve mapping results
        Given graph API, Elasticsearch and Graph Store are running
        When I post a new mapping
        And I retrieve that mapping
        Then I should be able to see the resource in Elasticsearch.
#        TBD when we have a proper end to end workflow
#        Then I should get indexed data in JSON-LD from the Elasticsearch.

    Scenario: Delete map results
        Given graph API is running
        When I delete a mapping
        But I should not be able to retrieve that mapping
        Then the mapping result still exists in Elasticsearch.
