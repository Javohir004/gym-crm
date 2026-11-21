package com.epam.training.integration;

import com.epam.training.gym.workload.ServiceTokenProvider;
import com.epam.training.integration.HttpCalls.Result;
import com.epam.training.integration.IntegrationEnvironment.RunningService;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class WorkloadFlowSteps {

    private static final String QUEUE = "trainer.workload";
    private static final String DLQ = "DLQ.trainer.workload";
    private static final Duration WAIT = Duration.ofSeconds(10);

    private final RunningService gymCrm;
    private final RunningService workloadService;
    private final JmsTemplate jmsTemplate;

    private String traineeUsername;
    private String traineeToken;
    private String trainerUsername;
    private LocalDate trainingDate;
    private Result gymResponse;
    private Result workloadResponse;

    public WorkloadFlowSteps(@Qualifier("gymCrm") RunningService gymCrm,
                             @Qualifier("workloadService") RunningService workloadService,
                             JmsTemplate jmsTemplate) {
        this.gymCrm = gymCrm;
        this.workloadService = workloadService;
        this.jmsTemplate = jmsTemplate;
    }

    @Before
    public void drainDeadLetterQueue() {
        jmsTemplate.setReceiveTimeout(100);
        while (jmsTemplate.receive(DLQ) != null) {
        }
    }

    private static String uniqueLastName() {
        return "It" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String serviceToken() {
        return gymCrm.context().getBean(ServiceTokenProvider.class).getToken();
    }

    private Result workload(String path) throws Exception {
        return HttpCalls.send("GET", workloadService.baseUrl() + "/api/v1/workloads/" + path, serviceToken(), null);
    }

    private long workloadMinutes(String username, LocalDate date) throws Exception {
        Result result = workload(username + "/years/" + date.getYear() + "/months/" + date.getMonthValue());
        if (result.status() == 404) {
            return 0;
        }
        assertEquals(200, result.status(), result.body());
        return ((Number) JsonPath.read(result.body(), "$.trainingSummaryDuration")).longValue();
    }

    private void addTraining(String token, String trainer, int days, int minutes) throws Exception {
        trainingDate = LocalDate.now().plusDays(days);
        String json = """
                {"traineeUsername":"%s","trainerUsername":"%s","trainingName":"Integration session",
                 "trainingDate":"%s","trainingDuration":%d}""".formatted(traineeUsername, trainer, trainingDate,
                minutes);
        gymResponse = HttpCalls.send("POST", gymCrm.baseUrl() + "/api/trainings", token, json);
    }

    @Given("a trainee is registered in gym-crm")
    public void aTraineeIsRegistered() throws Exception {
        Result result = HttpCalls.send("POST", gymCrm.baseUrl() + "/api/trainees", null,
                "{\"firstName\":\"Flow\",\"lastName\":\"" + uniqueLastName() + "\"}");
        assertEquals(201, result.status(), result.body());
        traineeUsername = JsonPath.read(result.body(), "$.username");
        traineeToken = JsonPath.read(result.body(), "$.token");
    }

    @Given("a trainer with specialization {string} is registered in gym-crm")
    public void aTrainerIsRegistered(String specialization) throws Exception {
        Result result = HttpCalls.send("POST", gymCrm.baseUrl() + "/api/trainers", null,
                "{\"firstName\":\"Coach\",\"lastName\":\"" + uniqueLastName() + "\",\"specialization\":\""
                        + specialization + "\"}");
        assertEquals(201, result.status(), result.body());
        trainerUsername = JsonPath.read(result.body(), "$.username");
    }

    @When("the trainee adds a training {int} days from now for {int} minutes in gym-crm")
    public void theTraineeAddsATraining(int days, int minutes) throws Exception {
        addTraining(traineeToken, trainerUsername, days, minutes);
    }

    @When("the trainee adds a training {int} days from now for {int} minutes with trainer {string} in gym-crm")
    public void theTraineeAddsATrainingWithTrainer(int days, int minutes, String trainer) throws Exception {
        addTraining(traineeToken, trainer, days, minutes);
    }

    @When("a training for the trainer is added in gym-crm without a token")
    public void aTrainingIsAddedWithoutAToken() throws Exception {
        addTraining(null, trainerUsername, 3, 30);
    }

    @Given("the trainee has a training {int} days from now for {int} minutes that reached the workload service")
    public void theTraineeHasATrainingThatReachedTheWorkloadService(int days, int minutes) throws Exception {
        theTraineeAddsATraining(days, minutes);
        assertEquals(200, gymResponse.status(), gymResponse.body());
        theWorkloadServiceShowsMinutes(minutes);
    }

    @When("the trainee deletes their profile in gym-crm")
    public void theTraineeDeletesTheirProfile() throws Exception {
        gymResponse = HttpCalls.send("DELETE", gymCrm.baseUrl() + "/api/trainees/" + traineeUsername, traineeToken,
                null);
    }

    @When("a workload message for the trainer is put on the queue with a token not issued by gym-crm")
    public void aForgedMessageIsSent() {
        String forged = Jwts.builder().subject("gym-crm").issuer("gym-crm").claim("scope", "workload")
                .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(5))))
                .signWith(Keys.hmacShaKeyFor("forged-secret-forged-secret-forged-secret-1234"
                        .getBytes(StandardCharsets.UTF_8)))
                .compact();
        String body = """
                {"trainerUsername":"%s","trainerFirstName":"Coach","trainerLastName":"Forged","isActive":true,
                 "trainingDate":"%s","trainingDuration":120,"actionType":"ADD"}"""
                .formatted(trainerUsername, LocalDate.now().plusDays(3));
        jmsTemplate.convertAndSend(QUEUE, body, message -> {
            message.setStringProperty("serviceToken", forged);
            return message;
        });
    }

    @When("the workload service is called with the trainee's gym-crm token")
    public void theWorkloadServiceIsCalledWithTheTraineeToken() throws Exception {
        workloadResponse = HttpCalls.send("GET", workloadService.baseUrl() + "/api/v1/workloads/" + trainerUsername,
                traineeToken, null);
    }

    @Then("gym-crm responds with status {int}")
    public void gymCrmRespondsWithStatus(int status) {
        assertEquals(status, gymResponse.status(), gymResponse.body());
    }

    @Then("the workload service responds with status {int}")
    public void theWorkloadServiceRespondsWithStatus(int status) {
        assertEquals(status, workloadResponse.status(), workloadResponse.body());
    }

    @Then("the workload service shows {int} minutes for the trainer in the month of the training")
    public void theWorkloadServiceShowsMinutes(int minutes) throws Exception {
        Instant deadline = Instant.now().plus(WAIT);
        long actual = -1;
        while (Instant.now().isBefore(deadline)) {
            actual = workloadMinutes(trainerUsername, trainingDate);
            if (actual == minutes) {
                return;
            }
            Thread.sleep(100);
        }
        fail("expected " + minutes + " minutes in the workload service but was " + actual);
    }

    @Then("the workload service has no summary for trainer {string}")
    public void theWorkloadServiceHasNoSummaryFor(String username) throws Exception {
        Thread.sleep(500);
        assertEquals(404, workload(username).status());
    }

    @Then("the workload service has no summary for the trainer")
    public void theWorkloadServiceHasNoSummaryForTheTrainer() throws Exception {
        theWorkloadServiceHasNoSummaryFor(trainerUsername);
    }

    @Then("the message is moved to the dead letter queue as not issued by gym-crm")
    public void theMessageIsMovedToTheDeadLetterQueue() throws JMSException {
        jmsTemplate.setReceiveTimeout(WAIT.toMillis());
        Message dead = jmsTemplate.receive(DLQ);
        assertNotNull(dead, "expected a message on " + DLQ);
        assertEquals("Missing or invalid service token", dead.getStringProperty("errorReason"));
    }
}
