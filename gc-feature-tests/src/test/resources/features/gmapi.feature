Feature: Handle mapping and indexing from the graph to Elasticsearch
    Scenario: Add map and get its status
        Given graph API, Elasticsearch and Graph Store are running
        When I post a mapping
        And the mapping is correct
        Then I should be able to retrieve status of mapping.

    Scenario: Retrieve mapping results
        Given graph API, Elasticsearch and GraphStore are running
        When I post a mapping
        And the mapping is correct
        Then I should get indexed data in JSON-LD from the Elasticsearch.

    Scenario: Delete map results
        Given graph API is running
        When I delete a mapping
        And the mapping still exists and not running
        Then I should not be able to retrieve that mapping.
