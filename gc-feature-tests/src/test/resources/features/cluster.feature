Feature: Handle clustering IDs in the graph store
    Scenario: Add map and get its status
        Given graph API and Graph Store are running
        When I run a clusterids job
        Then I should be able to see clustered IDs from Graph Store.
