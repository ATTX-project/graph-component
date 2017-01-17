/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.attx.gc.graphcomponent.test.stepdefinitions;

import cucumber.api.java8.En;

import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;

import org.apache.jena.query.*;
import org.json.JSONObject;

import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryExecutionFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author stefanne
 */
public class CronJobSteps implements En {
    private final String API_USERNAME = "master";
    private final String API_PASSWORD = "commander";
    private int workflow_id = 3;
    private int activity_id;
    private final String PIPELINE = "{ \n" +
            "\t\"name\": \"Pipeline\", \n" +
            "\t\"description\": \"Description\", \n" +
            "\t\"userExternalId\": \"http://www.johnadmin.cz\"\n" +
            "}";
    private final String ACTIVITY = "{\n" +
            "    \"debugging\": false,\n" +
            "     \"userExternalId\": \"http://www.johnadmin.cz\"\n" +
            "}";

    public CronJobSteps() {
        Given("^UnifiedViews, wfAPI and GraphStore are running$", () -> {
            try {
                GetRequest get = Unirest.get("http://localhost:8080/master/api/1/pipelines").basicAuth(API_USERNAME, API_PASSWORD).header("accept", "application/json");
                HttpResponse<JsonNode> response = get.asJson();
                int result = response.getStatus();
                assertEquals(result, 200);

                get = Unirest.get("http://localhost:4301/v0.1/workflow?format=json-ld");
                HttpResponse<JsonNode> response1 = get.asJson();
                int result1 = response1.getStatus();
                assertEquals(result1, 200);

                get = Unirest.get("http://localhost:3030/ds/get");
                HttpResponse<String> response2 = get.asString();
                int result2 = response2.getStatus();
                assertEquals(result2, 200);

            } catch (Exception ex) {
                Logger.getLogger(CronJobSteps.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }

        });

        When("^I add a Workflow$", () -> {
            try {
//                waiting for 2.3.1 UnifiedViews to test this
//                HttpResponse<JsonNode> postResponse = Unirest.post("http://localhost:8080/master/api/1/pipelines")
//                        .header("accept", "application/json")
//                        .header("Content-Type", "application/json")
//                        .basicAuth(API_USERNAME, API_PASSWORD)
//                        .body(PIPELINE)
//                        .asJson();
//
//                JSONObject myObj = postResponse.getBody().getObject();
//                pipeline_id = myObj.getString("id");

                String URL = String.format("http://localhost:8080/master/api/1/pipelines/%s", workflow_id);
                GetRequest get = Unirest.get(URL).basicAuth(API_USERNAME, API_PASSWORD).header("accept", "application/json");
                HttpResponse<JsonNode> response = get.asJson();
                JSONObject myObj = response.getBody().getObject();
                int the_id = myObj.getInt("id");
                assertEquals(the_id, 3);

            } catch (Exception ex) {
                Logger.getLogger(CronJobSteps.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }

        });


        When("^I run a Workflow$", () -> {
            try {
                String URL = String.format("http://localhost:8080/master/api/1/pipelines/%s/executions", workflow_id);
                HttpResponse<JsonNode> postResponse = Unirest.post(URL)
                        .header("accept", "application/json")
                        .header("Content-Type", "application/json")
                        .basicAuth(API_USERNAME, API_PASSWORD)
                        .body(ACTIVITY)
                        .asJson();

                JSONObject myObj = postResponse.getBody().getObject();
                activity_id = myObj.getInt("id");

            } catch (Exception ex) {
                Logger.getLogger(CronJobSteps.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });


        Then("^I should get \"([^\"]*)\" data in RDF from the GraphStore\\.$", (String arg1) -> {
            String activity_query =  String.format("PREFIX kaisa: <http://helsinki.fi/library/onto#> " +
                    "PREFIX prov: <http://www.w3.org/ns/prov#> " +
                    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                    "ASK{ GRAPH <http://localhost:3030/ds/data/provenance>{" +
                    "  kaisa:activity%d rdf:type ?object" +
                    "	} " +
                    "}", activity_id);
            String workflow_query =  String.format("PREFIX kaisa: <http://helsinki.fi/library/onto#> " +
                    "PREFIX prov: <http://www.w3.org/ns/prov#> " +
                    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                    "ASK{ GRAPH <http://localhost:3030/ds/data/provenance>{" +
                    "  kaisa:workflow%d rdf:type ?object" +
                    "	} " +
                    "}", workflow_id);
            try {
                String the_query = (arg1 == "activity") ? activity_query : workflow_query;
                Query query = QueryFactory.create(the_query);
                QueryExecution qexec = QueryExecutionFactory.sparqlService("http://localhost:3030/ds/query", query);
                Boolean result = qexec.execAsk();
                System.out.println(arg1);
                assertTrue(result);
                qexec.close() ;
            } catch (Exception ex) {
                Logger.getLogger(CronJobSteps.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }

        });

    }
}
