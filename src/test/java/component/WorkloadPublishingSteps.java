package component;

import com.epam.training.gym.workload.WorkloadMessagePublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class WorkloadPublishingSteps {

    private static final String QUEUE = "trainer.workload";

    private final MockMvc mockMvc;
    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;
    private final GymWorld world;
    private Message message;

    public WorkloadPublishingSteps(MockMvc mockMvc, JmsTemplate jmsTemplate, ObjectMapper objectMapper,
                                   GymWorld world) {
        this.mockMvc = mockMvc;
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
        this.world = world;
    }

    @Before
    public void drainQueue() {
        jmsTemplate.setReceiveTimeout(100);
        while (jmsTemplate.receive(QUEUE) != null) {
        }
    }

    private void addTraining(String token, String trainer, String name, LocalDate date, int minutes)
            throws Exception {
        String json = """
                {"traineeUsername":"%s","trainerUsername":"%s",%s"trainingDate":"%s","trainingDuration":%d}"""
                .formatted(world.trainee().username(), trainer, name == null ? "" : "\"trainingName\":\"" + name + "\",",
                        date, minutes);
        var request = post("/api/trainings").contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        world.response(mockMvc.perform(request).andReturn().getResponse());
    }

    @When("the trainee adds a training {string} {int} days from now for {int} minutes")
    public void theTraineeAddsATraining(String name, int days, int minutes) throws Exception {
        addTraining(world.trainee().token(), world.trainer().username(), name, LocalDate.now().plusDays(days),
                minutes);
    }

    @When("the trainee adds a training {string} with trainer {string} {int} days from now for {int} minutes")
    public void theTraineeAddsATrainingWithTrainer(String name, String trainer, int days, int minutes)
            throws Exception {
        addTraining(world.trainee().token(), trainer, name, LocalDate.now().plusDays(days), minutes);
    }

    @When("the trainee adds a training without a name")
    public void theTraineeAddsATrainingWithoutAName() throws Exception {
        addTraining(world.trainee().token(), world.trainer().username(), null, LocalDate.now().plusDays(1), 30);
    }

    @When("a training is added without a token")
    public void aTrainingIsAddedWithoutAToken() throws Exception {
        addTraining(null, world.trainer().username(), "No token", LocalDate.now().plusDays(1), 30);
    }

    @Given("the trainee has a training {string} {int} days from now for {int} minutes")
    public void theTraineeHasAnUpcomingTraining(String name, int days, int minutes) throws Exception {
        theTraineeAddsATraining(name, days, minutes);
        assertEquals(200, world.response().getStatus());
        drainQueue();
    }

    @Given("the trainee has a training {string} {int} days ago for {int} minutes")
    public void theTraineeHasAPastTraining(String name, int days, int minutes) throws Exception {
        theTraineeHasAnUpcomingTraining(name, -days, minutes);
    }

    @When("the trainee deletes their profile")
    public void theTraineeDeletesTheirProfile() throws Exception {
        world.response(mockMvc.perform(delete("/api/trainees/" + world.trainee().username())
                .header("Authorization", world.trainee().bearer())).andReturn().getResponse());
    }

    @Then("a {string} workload message for the trainer is published with duration {int}")
    public void aWorkloadMessageIsPublished(String action, int minutes) throws Exception {
        jmsTemplate.setReceiveTimeout(2000);
        message = jmsTemplate.receive(QUEUE);
        assertNotNull(message, "expected a message on " + QUEUE);
        JsonNode body = objectMapper.readTree(((TextMessage) message).getText());
        assertEquals(action, body.get("actionType").asText());
        assertEquals(world.trainer().username(), body.get("trainerUsername").asText());
        assertEquals(minutes, body.get("trainingDuration").asInt());
    }

    @Then("the workload message carries a service token and the trainer as group id")
    public void theWorkloadMessageCarriesAServiceToken() throws Exception {
        assertFalse(message.getStringProperty(WorkloadMessagePublisher.TOKEN_PROPERTY).isBlank());
        assertEquals(world.trainer().username(), message.getStringProperty(WorkloadMessagePublisher.GROUP_ID_PROPERTY));
    }

    @Then("no workload message is published")
    public void noWorkloadMessageIsPublished() {
        jmsTemplate.setReceiveTimeout(500);
        assertNull(jmsTemplate.receive(QUEUE));
    }
}
