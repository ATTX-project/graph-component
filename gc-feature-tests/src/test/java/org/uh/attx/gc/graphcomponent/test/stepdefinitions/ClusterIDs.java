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
    
    public ClusterIDs(){
        
        
        Given("^that required services are running$", () -> {
            try {
                // GMAPI                
                GetRequest get = Unirest.get(s.getGmapi()+ "/0.1/");
                HttpResponse<JsonNode> response1 = get.asJson();
                int result1 = response1.getStatus();
                assertEquals(404, result1);
                // FUSEKI                
                get = Unirest.get(s.getFuseki() + "/ds/get");
                HttpResponse<String> response3 = get.asString();
                int result3 = response3.getStatus();
                assertEquals(200, result3);

                
            } catch (Exception ex) {
                Logger.getLogger(ClusterIDs.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });

        When("^I run a clusterids job$", () -> {
            try {
                
                // add provenance data to the http://data.hulib.helsinki.fi/attx/prov graph
                String payload = IOUtils.toString(ClusterIDs.class.getResourceAsStream("/data/prov.trig"), "UTF-8");
                
                HttpResponse<JsonNode> provResponse = Unirest.put(s.getFuseki() + "/ds/data")
                        .header("Content-type", "application/trig")                        
                        .body(payload)                        
                        .asJson();       
                assertEquals(2, provResponse.getBody().getObject().get("count"));
                
                // run the clustering
                HttpResponse<JsonNode> postResponse = Unirest.post(s.getGmapi() + "/0.1/cluster")
                        .header("content-type", "application/json")
                        .asJson();
                JSONObject myObj = postResponse.getBody().getObject();
                this.statusCode = postResponse.getStatus();
                this.message = myObj.getString("title");
                
                
            } catch (Exception ex) {
                Logger.getLogger(ClusterIDs.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });

        Then("^I should get error message$", () -> {
            assertEquals(422, this.statusCode);
            assertNotEquals("", this.message );
        });
        
        Then("^I should get the status processed$", () -> {
            assertEquals( "Processed",status);
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
                
            
                // drop ids graph
                //HttpResponse<String> deleteResponse = Unirest.delete(s.getFuseki() + "/ds/data?graph=http://data.hulib.helsinki.fi/attx/ids").asString();
            } catch (Exception ex) {
                Logger.getLogger(ClusterIDs.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }  
        });

        Given("^the graph contains data for the test case (\\d+)$", (Integer testCase) -> {
            
            try {
                String endpoint = s.getFuseki() + "/ds/data";
                // drop graph working graph
                HttpResponse<String> deleteResponse = Unirest.delete(endpoint + "?graph=http://test/" + testCase).asString();

                // add graph
                String payload = IOUtils.toString(ClusterIDs.class.getResourceAsStream("/data/testcase" + testCase + ".trig"), "UTF-8");
                
                HttpResponse<String> response = Unirest.put(endpoint)
                        .header("Content-type", "application/trig")
                        .body(payload)                        
                        .asString();
                int result3 = response.getStatus();
                assertEquals(200, result3);
                
                
            } catch (Exception ex) {
                Logger.getLogger(ClusterIDs.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }            

        });



    }
}
