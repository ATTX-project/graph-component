/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.attx.gc.graphcomponent.integration.gc;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.flush.FlushResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import org.uh.attx.gc.graphcomponent.integration.PlatformServices;
import org.uh.attx.gc.graphcomponent.test.stepdefinitions.ClusterIDs;
import org.uh.attx.graphmanager.rdf2json.indexer.Indexer;

/**
 *
 * @author jkesanie
 */
public class IndexerIT {

    final static PlatformServices s = new PlatformServices(false);
    static String storeEndpoint = s.getFuseki() + "/ds/";
    static String indexEndpoint = "essiren";
    int bulkSize = 100;
    static String mapping = null;
    static TransportClient client = null;

    public IndexerIT() throws Exception {

    }

    @BeforeClass
    public static void setUpClass() {
        try {
                String payload = IOUtils.toString(IndexerIT.class.getResourceAsStream("/data/infras.ttl"), "UTF-8");                
                HttpResponse<JsonNode> response = Unirest.post(s.getFuseki() + "/ds/data?graph=http://test/es")
                        .header("Content-type", "text/turtle")
                        .body(payload)
                        .asJson();
                
                mapping = IOUtils.toString(IndexerIT.class.getResourceAsStream("/data/mapping-uc1.json"), "UTF-8");

                client = Indexer.getClient(indexEndpoint, 9300, "elasticsearch");
                
                
                // cleaup index current
                response = Unirest.delete(s.getESSiren() + "/current")
                        .asJson();
                
                
                
        }catch(Exception ex) {
            fail(ex.getMessage());
        }
        
    }

    @AfterClass
    public static void tearDownClass() {
        try {
            HttpResponse<String> deleteResponse1 = Unirest.post(s.getFuseki() + "/ds/update")
                    .header("Content-Type", "application/sparql-update")
                    .body("drop graph <http://test/es>")
                    .asString();

        } catch (Exception ex) {
            fail(ex.getMessage());
        }        
    }

    @Before
    public void setUp() {
        
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testMap() throws Exception {
        try {
            String[] inputGraphs = new String[] {"http://test/es"};

            Indexer i = new Indexer(storeEndpoint, client, bulkSize);
            i.prepareData(inputGraphs);
//            Model testModel = RDFDataMgr.loadModel("infras.ttl");
  //          i.setModel(testModel);
            i.map(inputGraphs, mapping, bulkSize);

            // flushing the values
            client.admin().indices().flush(new FlushRequest("current"), new ActionListener<FlushResponse>() {
                @Override
                public void onResponse(FlushResponse rspns) {
                    try {
                        HttpResponse<com.mashape.unirest.http.JsonNode> jsonResponse = Unirest.get(s.getESSiren() + "/current/_search?q=Finnish")
                                .asJson();

                        Assert.assertTrue(jsonResponse.getBody().getObject().getJSONObject("hits").getJSONArray("hits").length() > 0);
                    } catch (UnirestException ex) {
                        Logger.getLogger(IndexerIT.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }

                @Override
                public void onFailure(Throwable thrwbl) {
                    thrwbl.printStackTrace();
                    Assert.fail("Could not get documents from current index");
                }

            });

        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Mapping failed");
        }
    }

    @Test
    public void testPrepareData() throws Exception {
        Indexer i = new Indexer(storeEndpoint, client, bulkSize);
        i.prepareData(new String[] {"http://test/es"});

        Model m = i.getModel();

        assertFalse(m.isEmpty());

    }



}
