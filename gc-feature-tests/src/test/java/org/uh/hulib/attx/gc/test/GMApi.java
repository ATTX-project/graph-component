/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.hulib.attx.gc.test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
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

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * @author jkesanie
 */

public class GMApi {

    private static PlatformServices s = new PlatformServices();
    private static final String VERSION = "/0.1";
    private static final long startDelay = 1000;
    private static final long pollingInterval = 5000;



    @BeforeClass
    public static void setUpFuseki() {
        try {
            String payload = IOUtils.toString(GMApi.class.getResourceAsStream("/data/infras.ttl"), "UTF-8");
            HttpResponse<String> response = Unirest.post(s.getFuseki() + "/test/data?graph=http://test/index")
                    .header("Content-type", "text/turtle")
                    .body(payload)
                    .asString();

            //assertEquals(201, response.getStatus());

            HttpResponse<String> response2 = Unirest.post(s.getFuseki() + "/test/data?graph=http://test/index2")
                    .header("Content-type", "text/turtle")
                    .body(payload)
                    .asString();

            //assertEquals(201, response2.getStatus());
        } catch (Exception ex) {
            Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
            TestCase.fail(ex.getMessage());
        }
    }

    @BeforeClass
    public static void setUpElasticSearch() throws Exception {
        try {
//        EsSiren has a hard coded index named `current`
            HttpResponse<JsonNode> response = Unirest.delete(s.getESSiren() + "/current")
                    .asJson();

//        assertEquals(200, response.getStatus());

            response = Unirest.delete(s.getES5() + "/default")
                    .asJson();

//        assertEquals(200, response.getStatus());
        } catch (Exception ex) {
            Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
            TestCase.fail(ex.getMessage());
        }
    }

    @AfterClass
    public static void tearDown () {
        try {
            HttpResponse<String> deleteResponse1 = Unirest.post(s.getFuseki() + "/test/update")
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <http://test/index>")
                    .asString();

            HttpResponse<String> deleteResponse2 = Unirest.post(s.getFuseki() + "/test/update")
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <http://test/index2>")
                    .asString();

            HttpResponse<String> deleteResponse3 = Unirest.post(s.getFuseki() + "/test/update")
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <http://data.hulib.helsinki.fi/attx/prov>")
                    .asString();

            HttpResponse<JsonNode> response = Unirest.delete(s.getES5() + "/default")
                    .asJson();

            HttpResponse<JsonNode> response1 = Unirest.delete(s.getESSiren() + "/current")
                    .asJson();
        } catch (Exception ex) {
            Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
            TestCase.fail(ex.getMessage());
        }
    }

    @Test
    public void testGMEndpointsAvailable() {

        try {
            // index
            HttpResponse<JsonNode> response = Unirest.post(s.getGmapi() + VERSION + "/index")
                    .asJson();

            assertTrue(response.getStatus() >= 200);

            // prov
            response = Unirest.get(s.getGmapi() + VERSION + "/prov")
                    .asJson();

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

    private void pollForIndexing(int createdID) throws Exception {
        Timer timer = new Timer();
        final CountDownLatch latch = new CountDownLatch(1);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                HttpResponse<JsonNode> resp = null;
                try {
                    String URL = String.format(s.getGmapi() + VERSION + "/index/%s", createdID);
                    GetRequest get = Unirest.get(URL);
                    HttpResponse<JsonNode> response1 = get.asJson();
                    JSONObject myObj = response1.getBody().getObject();
                    String status = myObj.getString("status");
                    int result1 = response1.getStatus();
                    if (status.equalsIgnoreCase("Done")){
                        latch.countDown();
                        cancel();
                    } else if (status.equalsIgnoreCase("Error")) {
                        latch.countDown();
                        cancel();
                        fail("Polling returned Error status.");
                    }
                    assertThat(status, anyOf(is("WIP"), is("Done")));
                    assertEquals(200, result1);

                } catch (Exception ex) {
                    latch.countDown();
                    cancel();
                    fail(ex.getMessage());
                }
            }
        }, startDelay, pollingInterval);
        latch.await();
    }

    @Test
    public void testJavaIndexing() {
        doIndexing("/index_request_java.json", s.getESSiren(), "current");
    }

    @Test
    public void testPythonIndexing() {
        doIndexing("/index_request.json", s.getES5(), "default");
    }

    public void doIndexing(String requestFixture, String esEndpoint, String esIndex) {
        try {
            // index
            String indexPython = IOUtils.toString(GMApi.class.getResourceAsStream(requestFixture), "UTF-8");

            HttpResponse<JsonNode> postResponse = Unirest.post(s.getGmapi() + VERSION + "/index")
                    .header("content-type", "application/json")
                    .body(indexPython)
                    .asJson();
            JSONObject myObj = postResponse.getBody().getObject();
            int createdID = myObj.getInt("id");
            System.out.println(esEndpoint + ": "+ createdID);
            int result3 = postResponse.getStatus();
            assertEquals(202, result3);
            pollForIndexing(createdID);
            // query

            Unirest.post(esEndpoint + "/"+ esIndex +"/_refresh");
            Thread.sleep(5000);

            for(int i = 0; i < 10; i++) {
                HttpResponse<com.mashape.unirest.http.JsonNode> jsonResponse = Unirest.get(esEndpoint + "/"+ esIndex +"/_search?q=Finnish")
                        .asJson();

                JSONObject obj = jsonResponse.getBody().getObject();
                if(obj.has("hits")) {
                    int total = obj.getJSONObject("hits").getInt("total");
                    System.out.println(esEndpoint + ": "+ total);
                    assertTrue(total > 0);
                    return;
                }
                Thread.sleep(1000);
            }
            fail("Could not query indexing results");


            String URL = String.format(s.getGmapi() + VERSION + "/index/%s", createdID);
            HttpRequestWithBody request = Unirest.delete(URL);
            HttpResponse<String> response = request.asString();
            int result1 = response.getStatus();
            assertEquals(200, result1);

            GetRequest requestDelete = Unirest.get(URL);
            HttpResponse<String> response2 = requestDelete.asString();
            int result2 = response2.getStatus();
            assertEquals(410, result2);

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
            HttpResponse<JsonNode> response = Unirest.post(s.getFuseki() + "/test/data")
                    .header("Content-type", "application/trig")
                    .body(payload)
                    .asJson();

            int result3 = response.getStatus();
            assertEquals(9, response.getBody().getObject().get("count"));
            assertEquals(200, result3);

            // do clustering
            payload = "{ \"graphStore\": { \"host\": \"fuseki\", \"port\": 3030, \"dataset\": \"ds\" }}";
            HttpResponse<JsonNode> postResponse = Unirest.post(s.getGmapi() + "/0.1/cluster")
                    .header("content-type", "application/json")
                    .body(payload)
                    .asJson();
            JSONObject myObj = postResponse.getBody().getObject();
            
            int statusCode = postResponse.getStatus();
            String status = myObj.getString("status");

            assertEquals(200, statusCode);
            assertEquals("Processed", status);            
            
            // query results
            HttpResponse<JsonNode> queryResponse = Unirest.post(s.getFuseki() + "/test/query")
                    .header("Content-Type", "application/sparql-query")
                    .header("Accept", "application/sparql-results+json")
                    .body("SELECT (count(?o) as ?count) FROM <http://data.hulib.helsinki.fi/attx/ids> {?s <http://data.hulib.helsinki.fi/attx/id> ?o}")
                    .asJson();

            assertEquals(200, queryResponse.getStatus());
            assertEquals(3, queryResponse.getBody().getObject().getJSONObject("results").getJSONArray("bindings").getJSONObject(0).getJSONObject("count").getInt("value"));
            
            
        } catch (Exception ex) {
            Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
            TestCase.fail(ex.getMessage());
        } finally {
            clearClusterIdsData();
        }
        
    }
    
    private void clearClusterIdsData() {
        try {
            HttpResponse<String> response = Unirest.post(s.getFuseki() + "/test/update")
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <http://test/1>")
                    .asString();
            // drop prov graph
            HttpResponse<String> deleteResponse1 = Unirest.post(s.getFuseki() + "/test/update")
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <http://data.hulib.helsinki.fi/attx/prov>")
                    .asString();

            // drop ids graph
            HttpResponse<String> deleteResponse2 = Unirest.post(s.getFuseki() + "/test/update")
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <http://data.hulib.helsinki.fi/attx/ids>")
                    .asString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }    

    private final String API_USERNAME = "master";
    private final String API_PASSWORD = "commander";  
    private final String ACTIVITY = "{\n" +
            "    \"debugging\": false,\n" +
            "     \"userExternalId\": \"admin\"\n" +
            "}";    
    @Test
    public void testProvEndpoint() {
        try {
            // setup data
            
            // add pipeline
            URL resource = GMApi.class.getResource("/testPipeline.zip");
            
            HttpResponse<JsonNode> postResponse = Unirest.post(s.getUV() + "/master/api/1/pipelines/import")
                    .header("accept", "application/json")                        
                    .basicAuth(API_USERNAME, API_PASSWORD)
                    .field("importUserData", false)
                    .field("importSchedule", false)
                    .field("file", new File(resource.toURI()))                        
                    .asJson();            

            assertEquals(200, postResponse.getStatus());
            JSONObject myObj = postResponse.getBody().getObject();

            System.out.println(postResponse);

            int pipelineID = myObj.getInt("id");
            // run pipeline
            String URL = String.format(s.getUV() + "/master/api/1/pipelines/%s/executions", pipelineID);
            HttpResponse<JsonNode> postResponse2 = Unirest.post(URL)
                    .header("accept", "application/json")
                    .header("Content-Type", "application/json")
                    .basicAuth(API_USERNAME, API_PASSWORD)
                    .body(ACTIVITY)
                    .asJson();            
                
            assertEquals(200, postResponse2.getStatus());

            Thread.sleep(5000);
            
            // execute /prov
            HttpResponse<JsonNode> getResponse3 = Unirest.get(s.getGmapi() +  VERSION + "/prov?start=true&wfapi=" + s.getWfapi() + "/0.1&graphStore=" + s.getFuseki() + "/test")
                    .header("content-type", "application/json")
                    .asJson();
            JSONObject myObj3 = getResponse3.getBody().getObject();


            String status = myObj3.getString("status");
            String lastStart = myObj3.getString("lastStart");
            int result3 = getResponse3.getStatus();
            assertEquals(200, result3);
            assertEquals("Done", status);
            
            Thread.sleep(5000);
            
            
            // query results 
            HttpResponse<JsonNode> queryResponse = Unirest.post(s.getFuseki() + "/test/query")
                    .header("Content-Type", "application/sparql-query")
                    .header("Accept", "application/sparql-results+json")
                    .body("ASK\n" +
                    "FROM <http://data.hulib.helsinki.fi/attx/prov> \n" +
                    "{?s a <http://data.hulib.helsinki.fi/attx/onto#Workflow> \n" +
                    "}")
                    .asJson();

            HttpResponse<JsonNode> queryResponse2 = Unirest.post(s.getFuseki() + "/test/query")
                    .header("Content-Type", "application/sparql-query")
                    .header("Accept", "application/sparql-results+json")
                    .body("ASK\n" +
                    "FROM <http://data.hulib.helsinki.fi/attx/prov> \n" +
                    "{?s a <http://www.w3.org/ns/prov#Activity> \n" +
                    "}")
                    .asJson();
            System.out.println(queryResponse.getBody().getObject().getBoolean("boolean"));
            System.out.println(queryResponse2.getBody().getObject().getBoolean("boolean"));
            assertTrue(queryResponse.getBody().getObject().getBoolean("boolean"));
            assertTrue(queryResponse2.getBody().getObject().getBoolean("boolean"));
            
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
            HttpResponse<String> deleteResponse1 = Unirest.post(s.getFuseki() + "/test/update")
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <http://data.hulib.helsinki.fi/attx/prov>")
                    .asString();
            System.out.println("Delete prov :" + deleteResponse1.getStatusText());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }     
    
}
