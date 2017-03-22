package org.uh.hulib.attx.gc.test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import cucumber.api.java8.En;
import junit.framework.TestCase;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * Created by stefanne on 2/13/17.
 */
public class Provenance implements En {
    private PlatformServices s = new PlatformServices(false);
    
    private int statusCode;
    private String status;
    private String lastStart;

    public Provenance(){
        Given("^graph API, Workflow API and Graph Store are running$", () -> {
            try {
                GetRequest get = Unirest.get(s.getGmapi());
                HttpResponse<JsonNode> response1 = get.asJson();
                int result1 = response1.getStatus();
                assertEquals(404, result1);

                get = Unirest.get(s.getFuseki() + "/ds/get");
                HttpResponse<String> response3 = get.asString();
                int result3 = response3.getStatus();
                assertEquals(200, result3);

            } catch (Exception ex) {
                Logger.getLogger(Provenance.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });

        When("^I retrieve the provenance job status$", () -> {
            try {
                HttpResponse<JsonNode> postResponse = Unirest.post(s.getGmapi() + "/prov?wfapi=" + s.getWfapi() + "/0.1&graphStore=" + s.getFuseki() + "/test" )
                        .header("content-type", "application/json")
                        .asJson();
                 statusCode = postResponse.getStatus();
            } catch (Exception ex) {
                Logger.getLogger(Provenance.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });


        Then("^I should see no provenance jobs have been found\\.$", () -> {
            assertEquals(404, statusCode);
        });


        When("^I start a provenance update$", () -> {
            try {
                HttpResponse<JsonNode> postResponse = Unirest.post(s.getGmapi() + "/prov?start=true&?wfapi=" + s.getWfapi() + "/0.1&graphStore=" + s.getFuseki() + "/ds")
                        .header("content-type", "application/json")
                        .asJson();
                JSONObject myObj = postResponse.getBody().getObject();
                status = myObj.getString("status");
                lastStart = myObj.getString("lastStart");
                int result3 = postResponse.getStatus();
                assertEquals(200, result3);
            } catch (Exception ex) {
                Logger.getLogger(Provenance.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });


        Then("^I should obtain when the job was last started and its status\\.$", () -> {
            assertEquals(status, "Done");
//            Need to see how to compare the date
//            assertEquals(lastStart, "Done");
        });

        Then("^I should have provenance information in the Graph Store\\.$", () -> {
//            Need to see how to compare the date
//            assertEquals(lastStart, "Done");
        });
    }
    
}
