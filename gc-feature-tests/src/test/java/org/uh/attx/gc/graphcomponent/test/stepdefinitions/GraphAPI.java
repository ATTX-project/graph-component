package org.uh.attx.gc.graphcomponent.test.stepdefinitions;

import cucumber.api.PendingException;
import cucumber.api.java8.En;

/**
 * Created by stefanne on 1/31/17.
 */
public class GraphAPI implements En{
    public GraphAPI() {

        Given("^graph API, Elasticsearch and Graph Store are running$", () -> {
            // Write code here that turns the phrase above into concrete actions
            throw new PendingException();
        });

        When("^I post a mapping$", () -> {
            // Write code here that turns the phrase above into concrete actions
            throw new PendingException();
        });


        When("^the mapping is correct$", () -> {
            // Write code here that turns the phrase above into concrete actions
            throw new PendingException();
        });

        Then("^I should be able to retrieve status of mapping\\.$", () -> {
            // Write code here that turns the phrase above into concrete actions
            throw new PendingException();
        });

        Then("^I should get indexed data in JSON-LD from the Elasticsearch\\.$", () -> {
            // Write code here that turns the phrase above into concrete actions
            throw new PendingException();
        });

        Given("^graph API is running$", () -> {
            // Write code here that turns the phrase above into concrete actions
            throw new PendingException();
        });

        When("^I delete a mapping$", () -> {
            // Write code here that turns the phrase above into concrete actions
            throw new PendingException();
        });

        When("^the mapping still exists and not running$", () -> {
            // Write code here that turns the phrase above into concrete actions
            throw new PendingException();
        });

        Then("^I should not be able to retrieve that mapping\\.$", () -> {
            // Write code here that turns the phrase above into concrete actions
            throw new PendingException();
        });

    }
}
