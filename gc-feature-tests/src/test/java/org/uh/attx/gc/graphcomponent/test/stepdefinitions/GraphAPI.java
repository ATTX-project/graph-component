package org.uh.attx.gc.graphcomponent.test.stepdefinitions;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import cucumber.api.java8.En;
import junit.framework.TestCase;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Created by stefanne on 1/31/17.
 */
public class GraphAPI implements En{
    private int created_id1;
    private int created_id2;
    private int deleted_id;
    private final String REQUEST = "{\n  \"plugin\": \"python\",\n  \"targetEndpoint\": {\n  \t\"host\":\"localhost\",\n  \t\"port\": 9200\n  },\n  \"mapping\": {\n  \t\"index\": \"default\",\n  \t\"resourceType\": \"work\",\n    \"query\": \"CONSTRUCT {?subject ?predicate ?object} WHERE { ?subject ?predicate ?object } LIMIT 25\",\n    \"context\": {\"@context\": \"http://schema.org/\"}\n  },\n  \"sourceGraphs\": {\n  \t\"endpoint\": {\n  \t\t\"host\":\"localhost\", \n  \t\t\"port\":3030, \n  \t\t\"dataset\": \"ds\"\n  \t},\n  \t\"graphs\":[\"http://data.hulib.helsinki.fi/attx/ids\", \"http://data.hulib.helsinki.fi/attx/work1\"]\n  \t\n  },\n  \"format\": \"application/json+ld\"\n}";


    public GraphAPI() {

        Given("^graph API, Elasticsearch and Graph Store are running$", () -> {
            try {
                GetRequest get = Unirest.get("http://localhost:4302/");
                HttpResponse<JsonNode> response1 = get.asJson();
                int result1 = response1.getStatus();
//                If the server would not give 404 that means it does not run
                assertEquals(result1, 404);

                get = Unirest.get("http://localhost:9200/");
                HttpResponse<String> response2 = get.asString();
                int result2 = response2.getStatus();
                assertEquals(result2, 200);

                get = Unirest.get("http://localhost:3030/ds/get");
                HttpResponse<String> response3 = get.asString();
                int result3 = response3.getStatus();
                assertEquals(result3, 200);

            } catch (Exception ex) {
                Logger.getLogger(GraphAPI.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }

        });

        When("^I post a mapping$", () -> {
            try {
                HttpResponse<JsonNode> postResponse = Unirest.post("http://localhost:4302/map")
                        .header("content-type", "application/json")
                        .body(REQUEST)
                        .asJson();
                JSONObject myObj = postResponse.getBody().getObject();
                created_id1 = myObj.getInt("id");
                int result3 = postResponse.getStatus();
                assertEquals(result3, 202);
            } catch (Exception ex) {
                Logger.getLogger(GraphAPI.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });

        When("^I post a new mapping$", () -> {
            try {
                HttpResponse<JsonNode> postResponse = Unirest.post("http://localhost:4302/map")
                        .header("content-type", "application/json")
                        .body(REQUEST)
                        .asJson();
                JSONObject myObj = postResponse.getBody().getObject();
                created_id2 = myObj.getInt("id");
                int result3 = postResponse.getStatus();
                assertEquals(result3, 202);
            } catch (Exception ex) {
                Logger.getLogger(GraphAPI.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });

        Then("^I should be able to retrieve status of mapping\\.$", () -> {
            try {
                String URL = String.format("http://localhost:4302/map/%s", created_id1);
                GetRequest get = Unirest.get(URL);
                HttpResponse<JsonNode> response1 = get.asJson();
                JSONObject myObj = response1.getBody().getObject();
                String status = myObj.getString("status");
                int result1 = response1.getStatus();
                assertThat(status, anyOf(is("WIP"), is("Done")));
                assertEquals(result1, 200);
            } catch (Exception ex) {
                Logger.getLogger(GraphAPI.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });

        Given("^graph API is running$", () -> {
            try {
                GetRequest get = Unirest.get("http://localhost:4302/");
                HttpResponse<JsonNode> response1 = get.asJson();
                int result1 = response1.getStatus();
//                If the server would not give 404 that means it does not run
                assertEquals(result1, 404);

            } catch (Exception ex) {
                Logger.getLogger(GraphAPI.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });

        Then("^I should be able to see the resource in Elasticsearch\\.$", () -> {
            try {
                String URL = String.format("http://localhost:9200/default/work/%s", created_id2);
//                Wait for thread to execute
                Thread.sleep(5000);
                GetRequest get = Unirest.get(URL);
                HttpResponse<JsonNode> response1 = get.asJson();
                int result1 = response1.getStatus();
                assertEquals(result1, 200);
            } catch (Exception ex) {
                Logger.getLogger(GraphAPI.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });

        And("^I retrieve that mapping$", () -> {
            try {
                String URL = String.format("http://localhost:4302/map/%s", created_id2);
                GetRequest get = Unirest.get(URL);
                HttpResponse<JsonNode> response1 = get.asJson();
                int result1 = response1.getStatus();
                assertEquals(result1, 200);
            } catch (Exception ex) {
                Logger.getLogger(GraphAPI.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });


        When("^I delete a mapping$", () -> {
            try {
                HttpResponse<JsonNode> postResponse = Unirest.post("http://localhost:4302/map")
                        .header("content-type", "application/json")
                        .body(REQUEST)
                        .asJson();
                JSONObject myObj = postResponse.getBody().getObject();
                deleted_id = myObj.getInt("id");
//                Wait for thread to execute
                Thread.sleep(5000);
                String URL = String.format("http://localhost:4302/map/%s", deleted_id);
                HttpRequestWithBody request = Unirest.delete(URL);
                HttpResponse<String> response = request.asString();
                int result1 = response.getStatus();
                assertEquals(result1, 200);
            } catch (Exception ex) {
                Logger.getLogger(GraphAPI.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });


        But("^I should not be able to retrieve that mapping$", () -> {
            String URL = String.format("http://localhost:4302/map/%s", deleted_id);
            try {
                GetRequest get = Unirest.get(URL);
                HttpResponse<JsonNode> response1 = get.asJson();
                int result1 = response1.getStatus();
                assertEquals(result1, 410);
            } catch (Exception ex) {
                Logger.getLogger(GraphAPI.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });

        Then("^the mapping result still exists in Elasticsearch\\.$", () -> {
            try {
                String URL = String.format("http://localhost:9200/default/work/%s", deleted_id);
//                Wait for thread to execute
                Thread.sleep(5000);
                GetRequest get = Unirest.get(URL);
                HttpResponse<JsonNode> response1 = get.asJson();
                int result1 = response1.getStatus();
                assertEquals(result1, 200);
            } catch (Exception ex) {
                Logger.getLogger(GraphAPI.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });



    }
}
