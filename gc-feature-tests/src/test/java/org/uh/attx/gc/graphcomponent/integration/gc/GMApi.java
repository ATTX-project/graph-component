/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.attx.gc.graphcomponent.integration.gc;

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
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.uh.attx.gc.graphcomponent.integration.PlatformServices;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * @author jkesanie
 */
public class GMApi {

    private static PlatformServices s = new PlatformServices(false);
    private static final String VERSION = "/0.1";
    private static final long startDelay = 1000;
    private static final long pollingInterval = 5000;



    @BeforeClass
    public static void setUpFuseki() throws Exception {
        String payload = IOUtils.toString(GMApi.class.getResourceAsStream("/data/infras.ttl"), "UTF-8");
        HttpResponse<String> response = Unirest.post(s.getFuseki() + "/test/data?graph=http://test/index")
                .header("Content-type", "text/turtle")
                .body(payload)
                .asString();

        assertEquals(201, response.getStatus());

        HttpResponse<String> response2 = Unirest.post(s.getFuseki() + "/test/data?graph=http://test/index2")
                .header("Content-type", "text/turtle")
                .body(payload)
                .asString();

        assertEquals(201, response2.getStatus());
    }

    @BeforeClass
    public static void setUpElasticSearch() throws Exception {
//        EsSiren has a hard coded index named `current`
        HttpResponse<JsonNode> response = Unirest.delete(s.getESSiren() + "/current")
                .asJson();

//        assertEquals(200, response.getStatus());

        response = Unirest.delete(s.getES5() + "/default")
                .asJson();

//        assertEquals(200, response.getStatus());
    }

    @AfterClass
    public static void tearDown () throws Exception {
        HttpResponse<String> deleteResponse1 = Unirest.post(s.getFuseki() + "/test/update")
                .header("Content-Type", "application/sparql-update")
                .body("drop graph <http://test/index>")
                .asString();

        HttpResponse<String> deleteResponse2 = Unirest.post(s.getFuseki() + "/test/update")
                .header("Content-Type", "application/sparql-update")
                .body("drop graph <http://test/index2>")
                .asString();


        HttpResponse<JsonNode> response = Unirest.delete(s.getES5() + "/default")
                .asJson();

        HttpResponse<JsonNode> response1 = Unirest.delete(s.getESSiren() + "/current")
                .asJson();
    }

    @Test
    public void testGMEndpointsAvailable() {

        try {
            // index
            HttpResponse<JsonNode> response = Unirest.post(s.getGmapi() + VERSION + "/index")
                    .asJson();

            assertEquals(400, response.getStatus());

            // prov
            response = Unirest.get(s.getGmapi() + VERSION + "/prov")
                    .asJson();

            assertEquals(404, response.getStatus());


            // cluster
            String clusterPayload = "{ \"graphStore\": { \"host\": \"fuseki\", \"port\": 3030, \"dataset\": \"test\" }}";
            response = Unirest.post(s.getGmapi() + VERSION + "/cluster")
                    .body(clusterPayload)
                    .asJson();

            assertEquals(200, response.getStatus());

        } catch (Exception ex) {
            Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
            TestCase.fail(ex.getMessage());
        }

    }

    private void pollForIndexing(int createdIDPython) throws Exception {
        Timer timer = new Timer();
        final CountDownLatch latch = new CountDownLatch(1);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                HttpResponse<JsonNode> resp = null;
                try {
                    String URL = String.format(s.getGmapi() + VERSION + "/index/%s", createdIDPython);
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
    public void testPythonIndexing() {
        doIndexing("/index_request.json", s.getES5(), "default");
    }

    @Test
    public void testJavaIndexing() {
        doIndexing("/index_request_java.json", s.getESSiren(), "current");
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
            int createdIDPython = myObj.getInt("id");
            int result3 = postResponse.getStatus();
            assertEquals(202, result3);
            pollForIndexing(createdIDPython);
            Thread.sleep(5000);
            HttpResponse<com.mashape.unirest.http.JsonNode> jsonResponse = Unirest.get(esEndpoint + "/"+ esIndex +"/_search?q=Finnish")
                    .asJson();
            assertTrue((jsonResponse.getBody().getObject().getJSONObject("hits").getInt("total")) == 5);


            String URL = String.format(s.getGmapi() + VERSION + "/index/%s", createdIDPython);
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
}
