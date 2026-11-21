package com.epam.training.workload.component;

import com.epam.training.workload.support.TestTokens;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class WorkloadApiSteps {

    private static final String BASE = "/api/v1/workloads";

    private final MockMvc mockMvc;
    private MockHttpServletResponse response;

    public WorkloadApiSteps(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private void perform(MockHttpServletRequestBuilder request, String token) throws Exception {
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        response = mockMvc.perform(request).andReturn().getResponse();
    }

    @When("the summary of {string} is requested with a valid token")
    public void theSummaryIsRequestedWithAValidToken(String username) throws Exception {
        perform(get(BASE + "/" + username), TestTokens.valid());
    }

    @When("the summary of {string} is requested without a token")
    public void theSummaryIsRequestedWithoutAToken(String username) throws Exception {
        perform(get(BASE + "/" + username), null);
    }

    @When("the summary of {string} is requested with an expired token")
    public void theSummaryIsRequestedWithAnExpiredToken(String username) throws Exception {
        perform(get(BASE + "/" + username), TestTokens.token(TestTokens.SECRET, "gym-crm", "workload",
                Instant.now().minus(1, ChronoUnit.MINUTES)));
    }

    @When("the summary of {string} is requested with a token signed by another secret")
    public void theSummaryIsRequestedWithAForeignToken(String username) throws Exception {
        perform(get(BASE + "/" + username), TestTokens.token("another-secret-that-is-long-enough-for-hmac-256",
                "gym-crm", "workload", Instant.now().plus(5, ChronoUnit.MINUTES)));
    }

    @When("the workload of {string} for {int} month {int} is requested with a valid token")
    public void theMonthlyWorkloadIsRequested(String username, int year, int month) throws Exception {
        perform(get(BASE + "/" + username + "/years/" + year + "/months/" + month), TestTokens.valid());
    }

    @When("trainers named {string} {string} are searched with a valid token")
    public void trainersAreSearched(String firstName, String lastName) throws Exception {
        perform(get(BASE).param("firstName", firstName).param("lastName", lastName), TestTokens.valid());
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertEquals(status, response.getStatus());
    }

    @Then("the response field {string} is {string}")
    public void theResponseFieldIs(String field, String value) throws Exception {
        assertEquals(value, JsonPath.read(response.getContentAsString(), "$." + field).toString());
    }

    @Then("the result contains trainer {string}")
    public void theResultContainsTrainer(String username) throws Exception {
        List<String> usernames = JsonPath.read(response.getContentAsString(), "$[*].trainerUsername");
        assertTrue(usernames.contains(username));
    }

    @Then("the result is empty")
    public void theResultIsEmpty() throws Exception {
        List<Object> result = JsonPath.read(response.getContentAsString(), "$");
        assertTrue(result.isEmpty());
    }
}
