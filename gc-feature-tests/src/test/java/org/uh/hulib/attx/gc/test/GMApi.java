/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.hulib.attx.gc.test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

/**
 * @author jkesanie
 */

public class GMApi {

    private static PlatformServices s = new PlatformServices();
    private static final String VERSION = "/0.1";

    private final String API_USERNAME = "master";
    private final String API_PASSWORD = "commander";
    private final String ACTIVITY = "{ \"debugging\" : \"false\", \"userExternalId\" : \"admin\" }";

    @BeforeClass
    public static void setUpFuseki() throws Exception {
            TestCase.fail("testing");
            String payload = IOUtils.toString(GMApi.class.getResourceAsStream("/data/infras.ttl"), "UTF-8");
            HttpResponse<String> fusekiSimpleGraph = Unirest.post(s.getFuseki() + "/test/data?graph=http://test/index")
                    .header("Content-type", "text/turtle")
                    .body(payload)
                    .asString();

            HttpResponse<String> fusekiSimpleGraph2 = Unirest.post(s.getFuseki() + "/test/data?graph=http://test/index2")
                    .header("Content-type", "text/turtle")
                    .body(payload)
                    .asString();
    }

    @BeforeClass
    public static void setUpElasticSearch() throws Exception {
        try {
//        EsSiren has a hard coded index named `current`
            HttpResponse<JsonNode> esSirenSetup = Unirest.delete(s.getESSiren() + "/current").asJson();

            HttpResponse<JsonNode> es5Setup = Unirest.delete(s.getES5() + "/default").asJson();

        } catch (Exception ex) {
            Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
            TestCase.fail(ex.getMessage());
        }
    }

    @AfterClass
    public static void tearDown () {
        try {
            String updateURL = s.getFuseki() + "/test/update";
            HttpResponse<String> deleteGraph = Unirest.post(updateURL)
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <http://test/index>")
                    .asString();

            HttpResponse<String> deleteGraph2 = Unirest.post(updateURL)
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <http://test/index2>")
                    .asString();

            HttpResponse<String> deleteGraphProv = Unirest.post(updateURL)
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <http://data.hulib.helsinki.fi/attx/prov>")
                    .asString();

            HttpResponse<JsonNode> removeES5Index = Unirest.delete(s.getES5() + "/default")
                    .asJson();

            HttpResponse<JsonNode> removeEsSirenIndex = Unirest.delete(s.getESSiren() + "/current")
                    .asJson();
        } catch (Exception ex) {
            Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
            TestCase.fail(ex.getMessage());
        }
    }

    @Test
    public void testGMEndpointsAvailable() {

        try {
            // health check
            HttpResponse<JsonNode> response = Unirest.get(s.getGmapi() + "/health").asJson();

            assertEquals(200, response.getStatus());

            // prov
            response = Unirest.get(s.getGmapi() + VERSION + "/prov").asJson();

            assertTrue(response.getStatus() >= 200);

            // cluster
            String clusterPayload = "{ \"graphStore\": { \"host\": \"fuseki\", \"port\": 3030, \"dataset\": \"test\" }}";
            response = Unirest.post(s.getGmapi() + VERSION + "/cluster")
                    .body(clusterPayload)
                    .asJson();

            assertTrue(response.getStatus() >= 200);

        } catch (Exception ex) {
            Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
            TestCase.fail(ex.getMessage());
        }

    }

    @Test
    public void testJavaIndexing() {
        doIndexing("/index_request_java.json", s.getESSiren(), "current");
    }

    @Test
    public void testPythonIndexing() {
        doIndexing("/index_request.json", s.getES5(), "default");
    }

    private Callable<String> pollForIndexStatus(Integer createdID) {
        return new Callable<String>() {
            public String call() throws Exception {
                String URL = String.format(s.getGmapi() + VERSION + "/index/%s", createdID);
                GetRequest get = Unirest.get(URL);
                HttpResponse<JsonNode> response1 = get.asJson();
                JSONObject myObj = response1.getBody().getObject();
                String status = myObj.getString("status");
                System.out.println(status);
                return status;
            }
        };
    }

    private Callable<Integer> waitForESResults(String esEndpoint, String esIndex) {
        return new Callable<Integer>() {
            public Integer call() throws Exception {
                int totalHits = 0;
                Unirest.post(esEndpoint + "/"+ esIndex +"/_refresh");
                HttpResponse<com.mashape.unirest.http.JsonNode> jsonResponse = Unirest.get(esEndpoint + "/"+ esIndex +"/_search?q=Finnish")
                        .asJson();

                JSONObject esObj = jsonResponse.getBody().getObject();
                if(esObj.has("hits")) {
                    totalHits = esObj.getJSONObject("hits").getInt("total");
                }
                System.out.println(esEndpoint + ": " + totalHits);
                return totalHits;
            }
        };
    }

    public void doIndexing(String requestFixture, String esEndpoint, String esIndex) {
        try {
            // index
            String indexPython = IOUtils.toString(GMApi.class.getResourceAsStream(requestFixture), "UTF-8");

            HttpResponse<JsonNode> postIndex = Unirest.post(s.getGmapi() + VERSION + "/index")
                    .header("content-type", "application/json")
                    .body(indexPython)
                    .asJson();
            JSONObject indexObj = postIndex.getBody().getObject();
            int createdID = indexObj.getInt("id");
            assertEquals(202, postIndex.getStatus());

            await().atMost(20, TimeUnit.SECONDS).until(pollForIndexStatus(createdID), equalTo("Done"));
            await().atMost(45, TimeUnit.SECONDS).until(waitForESResults(esEndpoint, esIndex), equalTo(5));

            String URL = String.format(s.getGmapi() + VERSION + "/index/%s", createdID);
            HttpRequestWithBody deleteIndex = Unirest.delete(URL);
            HttpResponse<String> deleteIndexResponse = deleteIndex.asString();
            assertEquals(200, deleteIndexResponse.getStatus());

            GetRequest requestDeleteID = Unirest.get(URL);
            HttpResponse<String> deletedIDResponse = requestDeleteID.asString();
            assertEquals(410, deletedIDResponse.getStatus());

        } catch (Exception ex) {
            Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
            TestCase.fail(ex.getMessage());
        }
    }
    
    @Test
    public void testClusterIDs() {
        try {
            // add data
            String payload = IOUtils.toString(GMApi.class.getResourceAsStream("/data/testcase1.trig"), "UTF-8");
            HttpResponse<JsonNode> graphData = Unirest.post(s.getFuseki() + "/test/data")
                    .header("Content-type", "application/trig")
                    .body(payload)
                    .asJson();

            assertEquals(9, graphData.getBody().getObject().get("count"));
            assertEquals(200, graphData.getStatus());

            // do clustering
            payload = "{ \"graphStore\": { \"host\": \"fuseki\", \"port\": 3030, \"dataset\": \"ds\" }}";
            HttpResponse<JsonNode> postCluster = Unirest.post(s.getGmapi() + VERSION + "/cluster")
                    .header("content-type", "application/json")
                    .body(payload)
                    .asJson();
            JSONObject clusterObj = postCluster.getBody().getObject();
            
            int statusCode = postCluster.getStatus();
            String status = clusterObj.getString("status");

            assertEquals(200, statusCode);
            assertEquals("Processed", status);            
            
            // query results
            HttpResponse<JsonNode> queryResponse = Unirest.post(s.getFuseki() + "/test/query")
                    .header("Content-Type", "application/sparql-query")
                    .header("Accept", "application/sparql-results+json")
                    .body("SELECT (count(?o) as ?count) FROM <http://data.hulib.helsinki.fi/attx/ids> {?s <http://data.hulib.helsinki.fi/attx/id> ?o}")
                    .asJson();
            JSONObject queryObject = queryResponse.getBody().getObject().getJSONObject("results");
            assertEquals(200, queryResponse.getStatus());
            assertEquals(3, queryObject.getJSONArray("bindings").getJSONObject(0).getJSONObject("count").getInt("value"));
            
            
        } catch (Exception ex) {
            Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
            TestCase.fail(ex.getMessage());
        } finally {
            clearClusterIdsData();
        }
        
    }
    
    private void clearClusterIdsData() {
        try {
            HttpResponse<String> deleteGraph = Unirest.post(s.getFuseki() + "/test/update")
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <http://test/1>")
                    .asString();
            // drop prov graph
            HttpResponse<String> deleteGraphProv = Unirest.post(s.getFuseki() + "/test/update")
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <http://data.hulib.helsinki.fi/attx/prov>")
                    .asString();

            // drop ids graph
            HttpResponse<String> deleteGraphIDs = Unirest.post(s.getFuseki() + "/test/update")
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <http://data.hulib.helsinki.fi/attx/ids>")
                    .asString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Callable<String> pollForWorkflowExecution(Integer pipelineID) {
        return new Callable<String>() {
            public String call() throws Exception {
                String URL = String.format(s.getUV() + "/master/api/1/pipelines/%s/executions/last", pipelineID.intValue());
                HttpResponse<JsonNode> schedulePipelineResponse = Unirest.get(URL)
                        .header("accept", "application/json")
                        .header("Content-Type", "application/json")
                        .basicAuth(API_USERNAME, API_PASSWORD)
                        .asJson();
                if (schedulePipelineResponse.getStatus() == 200) {
                    JSONObject execution = schedulePipelineResponse.getBody().getObject();
                    String status = execution.getString("status");
                    System.out.println(status);
                    return status;
                } else {
                    return "Not yet";
                }
            }
        };
    }

    private Callable<String> pollForWorkflowStart(Integer pipelineID) {
        return new Callable<String>() {
            public String call() throws Exception {
                String URL = String.format(s.getUV() + "/master/api/1/pipelines/%s/executions", pipelineID);
                HttpResponse<JsonNode> workflowStart = Unirest.post(URL)
                        .header("accept", "application/json")
                        .header("Content-Type", "application/json")
                        .basicAuth(API_USERNAME, API_PASSWORD)
                        .body(ACTIVITY)
                        .asJson();
                assertEquals(200, workflowStart.getStatus());
                JSONObject execution = workflowStart.getBody().getObject();
                String status = execution.getString("status");
                System.out.println(status);
                return status;
            }
        };
    }

    @Test
    public void testProvEndpoint() {
        try {
            // add pipeline
            URL resource = GMApi.class.getResource("/testPipeline.zip");

            HttpResponse<JsonNode> postResponse = Unirest.post(s.getUV() + "/master/api/1/pipelines/import")
                    .header("accept", "application/json")
                    .basicAuth(API_USERNAME, API_PASSWORD)
                    .field("importUserData", false)
                    .field("importSchedule", false)
                    .field("file", new File(resource.toURI()))
                    .asJson();

            JSONObject myObj = postResponse.getBody().getObject();

            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                System.out.println(postResponse.getStatus());
                assertEquals(200, postResponse.getStatus());
            });

            int pipelineID = myObj.getInt("id");

            await().atMost(20, TimeUnit.SECONDS).until(pollForWorkflowStart(pipelineID), equalTo("QUEUED"));
            await().atMost(20, TimeUnit.SECONDS).until(pollForWorkflowExecution(pipelineID), equalTo("FINISHED_SUCCESS"));
            
            // execute /prov
            HttpResponse<JsonNode> wfProv = Unirest.get(s.getGmapi() +  VERSION + "/prov?start=true&wfapi=" + s.getWfapi() + VERSION + "&graphStore=" + s.getFuseki() + "/test")
                    .header("content-type", "application/json")
                    .asJson();
            JSONObject provObj = wfProv.getBody().getObject();

            assertEquals(200, wfProv.getStatus());
            assertEquals("Done", provObj.getString("status"));

            // query results
            HttpResponse<JsonNode> queryActivities = Unirest.post(s.getFuseki() + "/test/query")
                    .header("Content-Type", "application/sparql-query")
                    .header("Accept", "application/sparql-results+json")
                    .body("ASK\n" +
                            "FROM <http://data.hulib.helsinki.fi/attx/prov> \n" +
                            "{?s a <http://www.w3.org/ns/prov#Activity> \n" +
                            "}")
                    .asJson();


            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                try {
                    System.out.println(queryActivities.getBody().getObject().getBoolean("boolean"));
                    assertTrue(queryActivities.getBody().getObject().getBoolean("boolean"));
                } catch (Exception ex) {
                    Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
                    TestCase.fail(ex.getMessage());
                }
            });

            HttpResponse<JsonNode> queryWorkflows = Unirest.post(s.getFuseki() + "/test/query")
                    .header("Content-Type", "application/sparql-query")
                    .header("Accept", "application/sparql-results+json")
                    .body("ASK\n" +
                            "FROM <http://data.hulib.helsinki.fi/attx/prov> \n" +
                            "{?s a <http://data.hulib.helsinki.fi/attx/onto#Workflow> \n" +
                            "}")
                    .asJson();

            await().atMost(10, TimeUnit.SECONDS).until(() -> {
                try {
                    System.out.println(queryWorkflows.getBody().getObject().getBoolean("boolean"));
                    assertTrue(queryWorkflows.getBody().getObject().getBoolean("boolean"));
                } catch (Exception ex) {
                    Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
                    TestCase.fail(ex.getMessage());
                }
            });
            
        } catch (Exception ex) {
            Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
            TestCase.fail(ex.getMessage());
        } finally {
            clearProvData();
        }
        
    }

   private void clearProvData() {
        try {
            // drop prov graph
            HttpResponse<String> deleteGraphProv = Unirest.post(s.getFuseki() + "/test/update")
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <http://data.hulib.helsinki.fi/attx/prov>")
                    .asString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }     
    
}
