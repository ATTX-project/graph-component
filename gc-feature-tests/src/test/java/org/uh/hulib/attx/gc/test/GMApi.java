/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.hulib.attx.gc.test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import java.net.URL;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.uh.hulib.attx.dev.TestUtils;

import static org.awaitility.Awaitility.await;
import org.awaitility.core.ConditionTimeoutException;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.*;


/**
 * @author jkesanie
 */

public class GMApi {

    
    private void clearClusterIdsData() {
        TestUtils.dropGraph("http://test/1");
        TestUtils.dropGraph("http://data.hulib.helsinki.fi/attx/prov");
        TestUtils.dropGraph("http://data.hulib.helsinki.fi/attx/ids");
    }

    public void doIndexing(String requestFixture, String esEndpoint, String esIndex) {
        try {
            fail("testing jenkins");
            // index
            String indexPython = IOUtils.toString(GMApi.class.getResourceAsStream(requestFixture), "UTF-8");

            
            HttpResponse<JsonNode> postIndex = Unirest.post(TestUtils.getGmapi() + TestUtils.VERSION + "/index")
                    .header("content-type", "application/json")
                    .body(indexPython)
                    .asJson();
            JSONObject indexObj = postIndex.getBody().getObject();
            int createdID = indexObj.getInt("id");
            assertEquals(202, postIndex.getStatus());

            await().atMost(20, TimeUnit.SECONDS).until(TestUtils.pollForIndexStatus(createdID), equalTo("Done"));
            await().atMost(45, TimeUnit.SECONDS).until(TestUtils.waitForESResults(esEndpoint, esIndex), greaterThan(0));

            String URL = String.format(TestUtils.getGmapi() + TestUtils.VERSION + "/index/%s", createdID);
            HttpRequestWithBody deleteIndex = Unirest.delete(URL);
            HttpResponse<String> deleteIndexResponse = deleteIndex.asString();
            assertEquals(200, deleteIndexResponse.getStatus());

            GetRequest requestDeleteID = Unirest.get(URL);
            HttpResponse<String> deletedIDResponse = requestDeleteID.asString();
            assertEquals(410, deletedIDResponse.getStatus());

        } catch (ConditionTimeoutException cex) {
                fail("Timeout exceeded." + cex.getMessage());
        } catch (Exception ex) {
            Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
            TestCase.fail(ex.getMessage());
        }
    }



    @BeforeClass
    public static void setUpFuseki() throws Exception {
            String payload = IOUtils.toString(GMApi.class.getResourceAsStream("/data/infras.ttl"), "UTF-8");
            HttpResponse<String> fusekiSimpleGraph = Unirest.post(TestUtils.getFuseki() + "/test/data?graph=http://test/index")
                    .header("Content-type", "text/turtle")
                    .body(payload)
                    .asString();

            HttpResponse<String> fusekiSimpleGraph2 = Unirest.post(TestUtils.getFuseki() + "/test/data?graph=http://test/index2")
                    .header("Content-type", "text/turtle")
                    .body(payload)
                    .asString();
    }

    @BeforeClass
    public static void setUpElasticSearch() throws Exception {
        try {
//        EsSiren has a hard coded index named `current`
            HttpResponse<JsonNode> esSirenSetup = Unirest.delete(TestUtils.getESSiren() + "/current").asJson();

            HttpResponse<JsonNode> es5Setup = Unirest.delete(TestUtils.getES5() + "/default").asJson();

        } catch (Exception ex) {
            Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
            TestCase.fail(ex.getMessage());
        }
    }

    @AfterClass
    public static void tearDown () {
        try {
            TestUtils.dropGraph("http://test/index");
            TestUtils.dropGraph("http://test/index2");
            TestUtils.dropGraph("http://data.hulib.helsinki.fi/attx/prov");
            
            HttpResponse<JsonNode> removeES5Index = Unirest.delete(TestUtils.getES5() + "/default")
                    .asJson();

            HttpResponse<JsonNode> removeEsSirenIndex = Unirest.delete(TestUtils.getESSiren() + "/current")
                    .asJson();
        } catch (Exception ex) {
            Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
            TestCase.fail(ex.getMessage());
        }
    }

    @Test
    public void testEndpointsHealthAvailable() {

        try {
            // health check
            TestUtils.testWfHealth();
            TestUtils.testGmHealth();

            // prov
            HttpResponse<JsonNode> provHealth = Unirest.get(TestUtils.getGmapi() + TestUtils.VERSION + "/prov").asJson();
            assertTrue(provHealth.getStatus() >= 200);

            // cluster
            String clusterPayload = "{ \"graphStore\": { \"host\": \"fuseki\", \"port\": 3030, \"dataset\": \"test\" }}";
            HttpResponse<JsonNode> clusterHealth = Unirest.post(TestUtils.getGmapi() + TestUtils.VERSION + "/cluster")
                    .body(clusterPayload)
                    .asJson();
            assertTrue(clusterHealth.getStatus() >= 200);

        } catch (Exception ex) {
            Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
            TestCase.fail(ex.getMessage());
        }

    }

    @Test
    public void testJavaIndexing() {
        doIndexing("/index_request_java.json", TestUtils.getESSiren(), "current");
    }

    @Test
    public void testPythonIndexing() {
        doIndexing("/index_request.json", TestUtils.getES5(), "default");
    }
    
    @Test
    public void testClusterIDs() {
        try {
            // add data
            String payload = IOUtils.toString(GMApi.class.getResourceAsStream("/data/testcase1.trig"), "UTF-8");
            HttpResponse<JsonNode> graphData = Unirest.post(TestUtils.getFuseki() + "/test/data")
                    .header("Content-type", "application/trig")
                    .body(payload)
                    .asJson();

            assertEquals(9, graphData.getBody().getObject().get("count"));
            assertEquals(200, graphData.getStatus());

            // do clustering
            payload = "{ \"graphStore\": { \"host\": \"fuseki\", \"port\": 3030, \"dataset\": \"test\" }}";
            HttpResponse<JsonNode> postCluster = Unirest.post(TestUtils.getGmapi() + TestUtils.VERSION + "/cluster")
                    .header("content-type", "application/json")
                    .body(payload)
                    .asJson();
            JSONObject clusterObj = postCluster.getBody().getObject();
            
            int statusCode = postCluster.getStatus();
            String status = clusterObj.getString("status");

            assertEquals(200, statusCode);
            assertEquals("Processed", status);            
            
            // query results
            String queryString = "SELECT (count(?o) as ?count) FROM <http://data.hulib.helsinki.fi/attx/ids> {?s <http://data.hulib.helsinki.fi/attx/id> ?o}";            
            HttpResponse<JsonNode> queryResponse = TestUtils.graphQueryResult(queryString);
            assertEquals(200, queryResponse.getStatus());
            assertEquals(3, TestUtils.getQueryResultField(queryResponse, "count").getInt("value"));
            
            
        } catch (Exception ex) {
            Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
            TestCase.fail(ex.getMessage());
        } finally {
            clearClusterIdsData();
        }
        
    }

    @Test
    public void testProvEndpoint() {
        try {
            // add pipeline
            URL resource = GMApi.class.getResource("/testPipeline.zip");
            
            int pipelineID = TestUtils.importPipeline(resource);

            await().atMost(20, TimeUnit.SECONDS).until(TestUtils.pollForWorkflowStart(pipelineID), equalTo(200));
            await().atMost(20, TimeUnit.SECONDS).until(TestUtils.pollForWorkflowExecution(pipelineID), equalTo("FINISHED_SUCCESS"));
            
            // execute /prov

//           TO DO We preset the url as it will always be like this inside the container network.
//           When the everything is finished this should be a fixture or part of the plugin/service discovery
            TestUtils.updateProv();

            // query results
            String activityQuery = "ASK\n" +
                            "FROM <http://data.hulib.helsinki.fi/attx/prov> \n" +
                            "{?s a <http://www.w3.org/ns/prov#Activity> \n" +
                            "}";
            TestUtils.askGraphStoreIfTrue(activityQuery);


            String workflowQuery = "ASK\n" +
                            "FROM <http://data.hulib.helsinki.fi/attx/prov> \n" +
                            "{?s a <http://data.hulib.helsinki.fi/attx/onto#Workflow> \n" +
                            "}";
            TestUtils.askGraphStoreIfTrue(workflowQuery);
            
        } catch (ConditionTimeoutException cex) {
                fail("Timeout exceeded." + cex.getMessage());
        } catch (Exception ex) {
            Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
            TestCase.fail(ex.getMessage());
        } finally {
            TestUtils.clearProvData();
        }
        
    }
    
}
