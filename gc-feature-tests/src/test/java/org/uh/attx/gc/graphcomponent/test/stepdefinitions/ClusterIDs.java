package org.uh.attx.gc.graphcomponent.test.stepdefinitions;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import cucumber.api.java8.En;
import junit.framework.TestCase;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

import static org.junit.Assert.*;
import org.uh.attx.gc.graphcomponent.test.PlatformServices;

/**
 * Created by stefanne on 1/31/17.
 */
public class ClusterIDs implements En {

    private PlatformServices s = new PlatformServices(true);
    private String status;
    private String message;
    private int statusCode;
    private int testCase;

    public ClusterIDs() {

        Given("^that required services are running$", () -> {
            try {
                // GMAPI                
                GetRequest get = Unirest.get(s.getGmapi() + "/0.1/");
                HttpResponse<JsonNode> response1 = get.asJson();
                int result1 = response1.getStatus();
                assertEquals(404, result1);
                // FUSEKI                
                get = Unirest.get(s.getFuseki() + "/ds/get");
                HttpResponse<String> response3 = get.asString();
                int result3 = response3.getStatus();
                assertEquals(200, result3);

                clearData();

            } catch (Exception ex) {
                Logger.getLogger(ClusterIDs.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });

        When("^I run a clusterids job$", () -> {
            try {

                // add provenance data to the http://data.hulib.helsinki.fi/attx/prov graph
                String payload = IOUtils.toString(ClusterIDs.class.getResourceAsStream("/data/prov.trig"), "UTF-8");

                HttpResponse<JsonNode> provResponse = Unirest.post(s.getFuseki() + "/ds/data")
                        .header("Content-type", "application/trig")
                        .body(payload)
                        .asJson();
                assertEquals(2, provResponse.getBody().getObject().get("count"));

                // run the clustering
                payload = "{ \"graphStore\": { \"host\": \"fuseki\", \"port\": 3030, \"dataset\": \"ds\" }}";
                HttpResponse<JsonNode> postResponse = Unirest.post(s.getGmapi() + "/0.1/cluster")
                        .header("content-type", "application/json")
                        .body(payload)
                        .asJson();
                JSONObject myObj = postResponse.getBody().getObject();

                this.statusCode = postResponse.getStatus();
                this.message = myObj.getString("status");

            } catch (Exception ex) {
                Logger.getLogger(ClusterIDs.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });

        Then("^I should get error message$", () -> {
            assertEquals(200, this.statusCode);
            assertNotEquals("", this.message);
        });

        Then("^I should get the status processed$", () -> {
            assertEquals(200, this.statusCode);
            assertEquals("Processed", this.message);
        });

        Then("^number of clustered ids is (\\d+)$", (Integer idCount) -> {
            try {
                // query for ids
                HttpResponse<JsonNode> queryResponse = Unirest.post(s.getFuseki() + "/ds/query")
                        .header("Content-Type", "application/sparql-query")
                        .header("Accept", "application/sparql-results+json")
                        .body("SELECT (count(?o) as ?count) FROM <http://data.hulib.helsinki.fi/attx/ids> {?s <http://data.hulib.helsinki.fi/attx/id> ?o}")
                        .asJson();

                assertEquals(200, queryResponse.getStatus());
                assertEquals(idCount.intValue(), queryResponse.getBody().getObject().getJSONObject("results").getJSONArray("bindings").getJSONObject(0).getJSONObject("count").getInt("value"));

                clearData();
            } catch (Exception ex) {
                Logger.getLogger(ClusterIDs.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            } finally {
                clearTestCaseData();
            }
        });

        Given("^the graph contains data for the test case (\\d+)$", (Integer testCase) -> {
            this.testCase = testCase;
            try {
                String endpoint = s.getFuseki() + "/ds/data";

                // add graph
                String payload = IOUtils.toString(ClusterIDs.class.getResourceAsStream("/data/testcase" + testCase + ".trig"), "UTF-8");
                System.out.println(payload);
                HttpResponse<JsonNode> response = Unirest.post(endpoint)
                        .header("Content-type", "application/trig")
                        .body(payload)
                        .asJson();
                int result3 = response.getStatus();
                assertEquals(7, response.getBody().getObject().get("count"));
                assertEquals(200, result3);

            } catch (Exception ex) {
                Logger.getLogger(ClusterIDs.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            } finally {
            }

        });

    }

    private void clearTestCaseData() {
        try {
            HttpResponse<String> deleteResponse1 = Unirest.post(s.getFuseki() + "/ds/update")
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <http://test/" + this.testCase + ">")
                    .asString();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void clearData() {
        try {
            // drop prov graph
            HttpResponse<String> deleteResponse1 = Unirest.post(s.getFuseki() + "/ds/update")
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <http://data.hulib.helsinki.fi/attx/prov>")
                    .asString();
            System.out.println("Delete prov :" + deleteResponse1.getStatusText());
            // drop ids graph
            HttpResponse<String> deleteResponse2 = Unirest.post(s.getFuseki() + "/ds/update")
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <http://data.hulib.helsinki.fi/attx/ids>")
                    .asString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
