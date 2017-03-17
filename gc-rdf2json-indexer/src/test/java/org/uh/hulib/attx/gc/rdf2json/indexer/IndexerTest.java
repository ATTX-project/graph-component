/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.hulib.attx.gc.rdf2json.indexer;

import com.github.jsonldjava.utils.JsonUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 *
 * @author jkesanie
 */
public class IndexerTest {
    Map<String, Object> mapping = null;
    
    
    public IndexerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {

    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        try {
            mapping = (Map<String, Object>)JsonUtils.fromInputStream(IndexerTest.class.getResourceAsStream("/mapping1.json"));
        } catch (IOException ex) {
            Logger.getLogger(IndexerTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    @After
    public void tearDown() {
        mapping = null;
    }


    /**
     * Test of handleTarget method, of class Indexer.
     * @throws java.lang.Exception
     */
    @Test
    public void testHandleTarget() throws Exception {
        System.out.println("handleTarget");
        Indexer instance = new Indexer("", null, 0);

        Model m1 = RDFDataMgr.loadModel("data1.ttl");
        instance.setModel(m1);

        
        String index = "";
        String typeURI = "http://example.org/Test";
        
        List<Object> configs = (List<Object>)this.mapping.get("configs");
        Map<String, Object> targets = (Map<String, Object>)((Map<String, Object>)configs.get(0)).get("targets");
        Map<String, Object> target = (Map<String, Object>)targets.get(typeURI);

       
        instance.handleTarget(index, typeURI, target);
        // TODO review the generated test code and remove the default call to fail.
        assertTrue(true);
        
    }

    /**
     * Test of handleLiteralProperty method, of class Indexer.
     * @throws java.lang.Exception
     */
    @Test
    public void testHandleLiteralProperty() throws Exception {
        Indexer instance = new Indexer("", null, 0);

        Model m1 = RDFDataMgr.loadModel("data1.ttl");
        instance.setModel(m1);

        System.out.println("handleLiteralProperty");
        XContentBuilder builder =  XContentFactory.jsonBuilder();        
        
        // string, functional, no default        
        builder.startObject();        
        instance.handleLiteralProperty(builder, m1.getResource("http://example.org/ex1"), "http://www.w3.org/2000/01/rdf-schema#label", "label", true, null);        
        builder.endObject();
        
        JSONAssert.assertEquals("{\"label\":\"test\"}", builder.string(), JSONCompareMode.NON_EXTENSIBLE);

        // string, non-functional, no default
        builder =  XContentFactory.jsonBuilder();
        builder.startObject();        
        instance.handleLiteralProperty(builder, m1.getResource("http://example.org/ex2"), "http://www.w3.org/2000/01/rdf-schema#label", "label", false, null);        
        builder.endObject();
        
        JSONAssert.assertEquals("{\"label\": [\"test\", \"test2\"]}", builder.string(), JSONCompareMode.NON_EXTENSIBLE);
        
        builder =  XContentFactory.jsonBuilder();
        builder.startObject();        
        instance.handleLiteralProperty(builder, m1.getResource("http://example.org/ex1"), "http://www.w3.org/2000/01/rdf-schema#label", "label", false, null);        
        builder.endObject();
        
        JSONAssert.assertEquals("{\"label\": [\"test\"]}", builder.string(), JSONCompareMode.NON_EXTENSIBLE);

        // string, functional, but the data contains multiple values.
        // --> take one and log warning
        builder =  XContentFactory.jsonBuilder();
        builder.startObject();        
        instance.handleLiteralProperty(builder, m1.getResource("http://example.org/ex2"), "http://www.w3.org/2000/01/rdf-schema#label", "label", true, null);        
        builder.endObject();
        JSONObject jobj = new JSONObject(builder.string());
        assertTrue(jobj.has("label"));
        
        // string, functional, default value for existing property (i.e. forced value)
        builder =  XContentFactory.jsonBuilder();
        builder.startObject();        
        instance.handleLiteralProperty(builder, m1.getResource("http://example.org/ex1"), "http://www.w3.org/2000/01/rdf-schema#label", "label", true, "forced label");        
        builder.endObject();
        
        JSONAssert.assertEquals("{\"label\": \"forced label\"}", builder.string(), JSONCompareMode.NON_EXTENSIBLE);
        
        // string, functional, default value for non-existing property (i.e. actual default value)
        builder =  XContentFactory.jsonBuilder();
        builder.startObject();        
        instance.handleLiteralProperty(builder, m1.getResource("http://example.org/ex1"), "http://www.w3.org/2000/01/rdf-schema#comment", "comment", true, "forced comment|default comment");        
        builder.endObject();
        
        
        JSONAssert.assertEquals("{\"comment\": \"default comment\"}", builder.string(), JSONCompareMode.NON_EXTENSIBLE);
     
        // int, functional
        builder =  XContentFactory.jsonBuilder();
        builder.startObject();        
        instance.handleLiteralProperty(builder, m1.getResource("http://example.org/ex1"), "http://example.org/integer", "int", true, null);        
        builder.endObject();
        
        JSONAssert.assertEquals("{\"int\": 1}", builder.string(), JSONCompareMode.NON_EXTENSIBLE);

        // decimal, functional
        builder =  XContentFactory.jsonBuilder();
        builder.startObject();        
        instance.handleLiteralProperty(builder, m1.getResource("http://example.org/ex1"), "http://example.org/decimal", "decimal", true, null);        
        builder.endObject();
        System.out.println(builder.string());
        JSONAssert.assertEquals("{\"decimal\": 2.2}", builder.string(), JSONCompareMode.NON_EXTENSIBLE);

        // double, functional
        builder =  XContentFactory.jsonBuilder();
        builder.startObject();        
        instance.handleLiteralProperty(builder, m1.getResource("http://example.org/ex1"), "http://example.org/double", "double", true, null);        
        builder.endObject();
        System.out.println(builder.string());
        JSONAssert.assertEquals("{\"double\": 4.2E9}", builder.string(), JSONCompareMode.NON_EXTENSIBLE);
        
        // TODO: add more tests for different types of literals
    }

    /**
     * Test of handleObjectProperty method, of class Indexer.
     * @throws java.lang.Exception
     */
    @Test
    public void testHandleObjectProperty() throws Exception {
        Indexer instance = new Indexer("", null, 0);

        Model m1 = RDFDataMgr.loadModel("data1.ttl");
        instance.setModel(m1);

        
        System.out.println("handleObjectProperty");
        XContentBuilder builder = XContentFactory.jsonBuilder();
        
        // no apitemplate, functional, not nested
        builder.startObject();        
        instance.handleObjectProperty(builder, null, m1.getResource("http://example.org/ex3"), "http://example.org/objectProperty", "objectProperty", true, null, null);
        builder.endObject();
        
        JSONAssert.assertEquals("{\"objectProperty\": \"http://example.org/ex2\"}", builder.string(), JSONCompareMode.NON_EXTENSIBLE);

        // no apitemplate, not functional, not nested
        builder = XContentFactory.jsonBuilder();
        builder.startObject();        
        instance.handleObjectProperty(builder, null, m1.getResource("http://example.org/ex3"), "http://example.org/objectProperty2", "objectProperty2", false, null, null);
        builder.endObject();
        
        JSONAssert.assertEquals("{\"objectProperty2\": [\"http://example.org/ex2\", \"http://example.org/ex1\"]}", builder.string(), JSONCompareMode.NON_EXTENSIBLE);
        
        // no apitemplate, functional, nested
        builder = XContentFactory.jsonBuilder();
        builder.startObject();        
        
        Map<String, Object> nestedMapping = (Map<String, Object>)JsonUtils.fromString("{\"dataProperties\":[ {\"sourceProperty\": \"http://example.org/someProperty\", \"targetProperty\": \"someProperty\", \"isFunctional\": true}]}");
        instance.handleObjectProperty(builder, null, m1.getResource("http://example.org/ex3"), "http://example.org/objectProperty", "objectProperty2", true, nestedMapping, null);
        builder.endObject();
        System.out.println(builder.string());
        JSONAssert.assertEquals("{\"objectProperty2\": { \"@id\": \"http://example.org/ex2\", \"someProperty\": \"someProperty\"}}", builder.string(), JSONCompareMode.NON_EXTENSIBLE);

    }

    /**
     * Test of traverseGraph method, of class Indexer.
     * @throws java.lang.Exception
     */
    @Test
    public void testTraverseGraph() throws Exception {
        
        System.out.println("traverseGraph");
        
        Indexer instance = new Indexer("",  null, 0);

        Model m1 = RDFDataMgr.loadModel("data1.ttl");
        instance.setModel(m1);

        Resource expResult = m1.getResource("http://example.org/ex2");
        String[] parts = "http://example.org/objectProperty->http://example.org/someProperty".split("->");
        Resource result = instance.traverseGraph(m1.getResource("http://example.org/ex3"), parts , 0);
        assertEquals(expResult, result);
        
        // non-functional property - should pick one
        parts = "http://example.org/objectProperty2->http://example.org/someProperty".split("->");
        result = instance.traverseGraph(m1.getResource("http://example.org/ex3"), parts , 0);
        assertNotNull(result);
        
    }

    /**
     * Test of handleObjectProperties method, of class Indexer.
     * @throws java.lang.Exception
     */
    @Test
    public void testHandleObjectProperties() throws Exception {
        System.out.println("handleObjectProperties");
        
        Indexer instance = new Indexer("", null, 0);
        Model m1 = RDFDataMgr.loadModel("data1.ttl");
        instance.setModel(m1);        
        XContentBuilder builder = XContentFactory.jsonBuilder();
        
        
        Map<String, Object> conf = (Map<String, Object>)JsonUtils.fromString("{\"objectProperties\":[ {\"sourceProperty\": \"http://example.org/objectProperty\", \"targetProperty\": \"objectProperty\", \"isFunctional\": true}, {\"sourceProperty\": \"http://example.org/objectProperty2\", \"targetProperty\": \"objectProperty2\", \"isFunctional\": false}]}");
        builder.startObject();  
        instance.handleObjectProperties(builder, null, m1.getResource("http://example.org/ex3"), conf);
        builder.endObject();
        JSONAssert.assertEquals("{\"objectProperty\": \"http://example.org/ex2\", \"objectProperty2\": [\"http://example.org/ex2\", \"http://example.org/ex1\"]}", builder.string(), JSONCompareMode.NON_EXTENSIBLE);
    }

    /**
     * Test of handleMultilingualDataProperties method, of class Indexer.
     * @throws java.lang.Exception
     */
    @Test
    public void testHandleMultilingualDataProperties() throws Exception {
        System.out.println("handleMultilingualDataProperties");
        
        Indexer instance = new Indexer("", null, 0);
        Model m1 = RDFDataMgr.loadModel("data1.ttl");
        instance.setModel(m1);        
        XContentBuilder builder = XContentFactory.jsonBuilder();

        
        List<Object> configs = (List<Object>)this.mapping.get("configs");
        Map<String, Object> targets = (Map<String, Object>)((Map<String, Object>)configs.get(0)).get("targets");
        Map<String, Object> target = (Map<String, Object>)targets.get("http://example.org/Test4");
        Map<String, Object> indexDoc = (Map<String, Object>)target.get("indexDoc");
        builder.startObject(); 
        instance.handleMultilingualDataProperties(builder, m1.getResource("http://example.org/ex4"), indexDoc);
        builder.endObject(); 
        JSONAssert.assertEquals("{\"label\":{\"fi\":\"testi\",\"en\":\"test\"}}", builder.string(), JSONCompareMode.NON_EXTENSIBLE);
        
        builder = XContentFactory.jsonBuilder();
       
        target = (Map<String, Object>)targets.get("http://example.org/Test5");
        indexDoc = (Map<String, Object>)target.get("indexDoc");
        builder.startObject(); 
        instance.handleMultilingualDataProperties(builder, m1.getResource("http://example.org/ex5"), indexDoc);
        builder.endObject();         
        JSONAssert.assertEquals("{\"label\":{\"fi\":[\"testi\"],\"en\":[\"test2\",\"test\"]}}", builder.string(), JSONCompareMode.NON_EXTENSIBLE);
        
        
       
    }

    /**
     * Test of handleDataProperties method, of class Indexer.
     * @throws java.lang.Exception
     */
    @Test
    public void testHandleDataProperties() throws Exception {
        System.out.println("handleDataProperties");
        
        Indexer instance = new Indexer("", null, 0);
        Model m1 = RDFDataMgr.loadModel("data1.ttl");
        instance.setModel(m1);        
        XContentBuilder builder = XContentFactory.jsonBuilder();
        
        
        Map<String, Object> conf = (Map<String, Object>)JsonUtils.fromString("{\"dataProperties\":[ {\"sourceProperty\": \"http://example.org/someProperty\", \"targetProperty\": \"someProperty\", \"isFunctional\": true}]}");
        builder.startObject();  
        instance.handleDataProperties(builder, m1.getResource("http://example.org/ex2"), conf);
        builder.endObject();
        JSONAssert.assertEquals("{\"someProperty\": \"someProperty\"}", builder.string(), JSONCompareMode.NON_EXTENSIBLE);
        
    }

    /**
     * Test of generateDocument method, of class Indexer.
     * @throws java.lang.Exception
     */
    @Test
    public void testGenerateDocument() throws Exception {
        System.out.println("generateDocument");
        Indexer instance = new Indexer("", null, 0);
        Model m1 = RDFDataMgr.loadModel("data1.ttl");
        instance.setModel(m1);        
        
        String typeURI = "http://example.org/Test";
        List<Object> configs = (List<Object>)this.mapping.get("configs");
        Map<String, Object> targets = (Map<String, Object>)((Map<String, Object>)configs.get(0)).get("targets");
        Map<String, Object> target = (Map<String, Object>)targets.get(typeURI);
        Map<String, Object> indexDoc = (Map<String, Object>)target.get("indexDoc");

        
        String idProperty = null;
        String id = "1";
        Resource r = m1.getResource("http://example.org/ex2");
        Map<String, Object> conf = target; 
        
        XContentBuilder builder = instance.generateDocument(idProperty, id, r, indexDoc);
        //System.out.println(result.string());
        
        
        JSONAssert.assertEquals("{\"@id\":\"1\",\"label\":\"test2\"}", builder.string(), JSONCompareMode.NON_EXTENSIBLE);

        
    }
    
}
