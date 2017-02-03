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
 * Created by stefanne on 1/31/17.
 */
public class ClusterIDs implements En {
    private String status;

    public ClusterIDs(){
        Given("^graph API and Graph Store are running$", () -> {
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

        When("^I run a clusterids job$", () -> {
            try {
                HttpResponse<JsonNode> postResponse = Unirest.post("http://localhost:4302/clusterids")
                        .header("content-type", "application/json")
                        .asJson();
                JSONObject myObj = postResponse.getBody().getObject();
                status = myObj.getString("status");
                int result3 = postResponse.getStatus();
                assertEquals(result3, 200);
            } catch (Exception ex) {
                Logger.getLogger(ClusterIDs.class.getName()).log(Level.SEVERE, null, ex);
                TestCase.fail(ex.getMessage());
            }
        });


        Then("^I should get the status processed\\.$", () -> {
            assertEquals(status, "Processed");
        });
    }
}
