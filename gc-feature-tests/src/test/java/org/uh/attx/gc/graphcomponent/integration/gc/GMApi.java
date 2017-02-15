/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.attx.gc.graphcomponent.integration.gc;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.junit.Test;
import org.uh.attx.gc.graphcomponent.integration.PlatformServices;
import static org.junit.Assert.*;

/**
 *
 * @author jkesanie
 */
public class GMApi {

    private PlatformServices s = new PlatformServices(false);
    private final String VERSION = "/0.1";
    
    @Test
    public void testGMEndpointsAvailable() {        
        
        try {
            // index
            HttpResponse<JsonNode> response = Unirest.get(s.getGmapi() + VERSION + "/map/1")
                    .asJson();
                       
            assertEquals(410, response.getStatus());

            // prov
            response = Unirest.get(s.getGmapi() + VERSION + "/prov")
                  .asJson();
            
            assertEquals(404, response.getStatus());
            
            
            // cluster
            String clusterPayload = "{ \"graphStore\": { \"host\": \"fuseki\", \"port\": 3030, \"dataset\": \"ds\" }}";
            response = Unirest.post(s.getGmapi() + VERSION + "/cluster")
                    .body(clusterPayload)
                    .asJson();
            
            assertEquals(200, response.getStatus());
            
        } catch (Exception ex) {
            Logger.getLogger(GMApi.class.getName()).log(Level.SEVERE, null, ex);
            TestCase.fail(ex.getMessage());
        }
        
    }
}
