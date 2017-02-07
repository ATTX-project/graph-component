/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.attx.gc.graphcomponent.integration.test.rdf2json;

import com.mashape.unirest.http.HttpResponse;
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
import org.uh.attx.graphmanager.rdf2json.indexer.Indexer;
import static org.junit.Assert.*;

/**
 *
 * @author jkesanie
 */
public class IndexerIT {

    String storeEndpoint = "http://localhost:3030/ds/";
    String indexEndpoint = "localhost";
    int bulkSize = 100;
    String mapping = null;
    TransportClient client = null;

    public IndexerIT() throws Exception {
        try {
            this.mapping = IOUtils.toString(IndexerIT.class.getResourceAsStream("/mapping-uc1.json"), "UTF-8");

            client = Indexer.getClient(indexEndpoint, 9300, "elasticsearch");
        } catch (Exception ex) {
            fail("Initialization failed." + ex.getMessage());
        }

    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of main method, of class Indexer.
     */
    @Test
    public void testMain() throws Exception {
        System.out.println("main");
        fail("Implement me!");
        //String[] args = null;
        //Indexer.main(args);
        // TODO review the generated test code and remove the default call to fail.

    }

    /**
     * Test of map method, of class Indexer.
     */
    @Test
    public void testMap() throws Exception {
        try {
            String[] inputGraphs = null;

            Indexer i = new Indexer(storeEndpoint, client, bulkSize);
            Model testModel = RDFDataMgr.loadModel("infras.ttl");
            i.setModel(testModel);
            i.map(inputGraphs, mapping, bulkSize);

            // flushing the values
            client.admin().indices().flush(new FlushRequest("current"), new ActionListener<FlushResponse>() {
                @Override
                public void onResponse(FlushResponse rspns) {
                    try {
                        HttpResponse<com.mashape.unirest.http.JsonNode> jsonResponse = Unirest.get("http://localhost:9200/current/_search?q=Finnish")
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

    /**
     * Test of prepareData method, of class Indexer.
     */
    @Test
    public void testPrepareData() throws Exception {
        Indexer i = new Indexer(storeEndpoint, client, bulkSize);
        i.prepareData(null);

        Model m = i.getModel();

        assertFalse(m.isEmpty());

    }

    /**
     * Test of switchIndices method, of class Indexer.
     */
    @Test
    public void testSwitchIndices() {
        System.out.println("switchIndices");
        fail("Implement me!");
        //String prefix = "";
        //String newIndex = "";
        //Indexer instance = null;
        //instance.switchIndices(prefix, newIndex);
        // TODO review the generated test code and remove the default call to fail.
    }

    /**
     * Test of createNewSirenIndex method, of class Indexer.
     */
    @Test
    public void testCreateNewSirenIndex() {
        System.out.println("createNewSirenIndex");
        fail("Implement me!");
        //String index = "";
        //Indexer instance = null;
        //boolean expResult = false;
        //boolean result = instance.createNewSirenIndex(index);
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
    }

    /**
     * Test of getCurrentIndex method, of class Indexer.
     */
    @Test
    public void testGetCurrentIndex() {
        System.out.println("getCurrentIndex");
        fail("Implement me!");
        //String prefix = "";
        //Indexer instance = null;
        //String expResult = "";
        //String result = instance.getCurrentIndex(prefix);
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
    }

}
