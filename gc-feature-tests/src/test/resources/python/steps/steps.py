import httplib

@Given('^UnifiedViews, wfAPI and GraphStore are running$')
def unifiedviews_wfAPI_and_GraphStore_are_running(self):
    # uv_request = requests.get("http://locahost:8080/master/api/1/",
    #                           auth=('master', 'commander'))
    # wfapi_request = requests.get("http://locahost:4301/v0.1/workflow")
    # graph_request = requests.get("http://localhost:3030/ds")
    # if (uv_request.status_code != 200 or wfapi_request.status_code != 200
    #         or graph_request.status_code != 200):
    conn = httplib.HTTPConnection("http://locahost:4301")
    conn.request("GET", "/v0.1/workflow")
    r1 = conn.getresponse()
    if r1.status != 200:
        raise(Exception("One of the endpoints is not running."))


@When('^I add a Workflow$')
def i_add_a_Workflow(self):
    # Write code here that turns the phrase above into concrete actions
    raise(Exception("I cannot add an workflow."))


@When('^I run a Workflow$')
def i_run_a_Workflow(self):
    # Write code here that turns the phrase above into concrete actions
    raise(Exception("I cannot run a Workflow"))


@Then('^I should get (.+) data in RDF from the GraphStore\.$')
def i_should_get_activity_data_in_RDF_from_the_GraphStore(self, data_type):
    # Write code here that turns the phrase above into concrete actions
    raise(Exception("data_type data does not match the data in the Graph."))
