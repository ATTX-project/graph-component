Feature: Handle clustering IDs in the graph store
    Scenario: Cluster ids with an empty graph
        Given that required services are running
        When I run a clusterids job
        Then I should get error message

    
    Scenario: Cluster ids for the test case 1
        Given  that required services are running 
        And the graph contains data for the test case 1
        When I run a clusterids job
        Then I should get the status processed
        And number of clustered ids is 3
