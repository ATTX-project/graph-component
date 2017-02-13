Feature: Handle updating and retrieving provenance data in the graph store
  Scenario: No provenance jobs performed
    Given graph API, workflow API and Graph Store are running
    When I retrieve the provenance status
    Then I should see no provenance jobs have been found.

  Scenario: Start provenance job performed
    Given graph API, workflow API and Graph Store are running
    When I start a provenance update
    Then I should obtain when the job was last started and its status.
