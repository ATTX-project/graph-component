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

import static org.junit.Assert.assertEquals;

/**
 * Created by stefanne on 2/13/17.
 */
public class Provenance implements En {
    private int statusCode;
    private String status;
    private String lastStart;

    public Provenance(){
        Given("^graph API, Workflow API and Graph Store are running$", () -> {
            try {
                GetRequest get = Unirest.get("http://localhost:4302/");
                HttpResponse<JsonNode> response1 = get.asJson();
                int result1 = response1.getStatus();
                assertEquals(result1, 404);

                get = Unirest.get("http://localhost:3030/ds/get");
                HttpResponse<String> response3 = get.asString();
                int result3 = response3.getStatus();
                assertEquals(result3, 200);

            } catch (Exception ex) {
                Logger.getLogger(ClusterIDs.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });

        When("^I retrieve the provenance job status$", () -> {
            try {
                HttpResponse<JsonNode> postResponse = Unirest.post("http://localhost:4302/prov")
                        .header("content-type", "application/json")
                        .asJson();
                 statusCode = postResponse.getStatus();
            } catch (Exception ex) {
                Logger.getLogger(ClusterIDs.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });


        Then("^I should see no provenance jobs have been found\\.$", () -> {
            assertEquals(statusCode, 404);
        });


        When("^I start a provenance update$", () -> {
            try {
                HttpResponse<JsonNode> postResponse = Unirest.post("http://localhost:4302/prov?start=true")
                        .header("content-type", "application/json")
                        .asJson();
                JSONObject myObj = postResponse.getBody().getObject();
                status = myObj.getString("status");
                lastStart = myObj.getString("lastStart");
                int result3 = postResponse.getStatus();
                assertEquals(result3, 200);
            } catch (Exception ex) {
                Logger.getLogger(ClusterIDs.class.getName()).log(Level.SEVERE, null, ex);
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
}
