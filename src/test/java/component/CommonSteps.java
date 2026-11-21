package component;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class CommonSteps {

    private final GymWorld world;

    public CommonSteps(GymWorld world) {
        this.world = world;
    }

    @Given("a trainee {string} {string} is registered")
    public void aTraineeIsRegistered(String firstName, String lastName) throws Exception {
        world.trainee(world.register("/api/trainees",
                "{\"firstName\":\"" + firstName + "\",\"lastName\":\"" + lastName + "\"}"));
    }

    @Given("a trainer {string} {string} with specialization {string} is registered")
    public void aTrainerIsRegistered(String firstName, String lastName, String specialization) throws Exception {
        world.trainer(world.register("/api/trainers", "{\"firstName\":\"" + firstName + "\",\"lastName\":\""
                + lastName + "\",\"specialization\":\"" + specialization + "\"}"));
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertEquals(status, world.response().getStatus());
    }

    @Then("the response field {string} is {string}")
    public void theResponseFieldIs(String field, String value) throws UnsupportedEncodingException {
        assertEquals(value, JsonPath.read(world.response().getContentAsString(), "$." + field).toString());
    }

    @Then("the response contains a token")
    public void theResponseContainsAToken() throws UnsupportedEncodingException {
        String token = JsonPath.read(world.response().getContentAsString(), "$.token");
        assertFalse(token.isBlank());
    }
}
