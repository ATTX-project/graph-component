Feature: Handle retrieving and storing in the graph data from UnifiedViews
    Scenario: Add Workflow and get its RDF version stored
        Given UnifiedViews, wfAPI and GraphStore are running
        When I add a Workflow
        Then I should get "workflow" data in RDF from the GraphStore.

    Scenario: Run Workflow and get its RDF version stored
        Given UnifiedViews, wfAPI and GraphStore are running
        When I run a Workflow
        Then I should get "activity" data in RDF from the GraphStore.
