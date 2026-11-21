package com.epam.training.workload.component;

import com.epam.training.workload.messaging.DeadLetterPublisher;
import com.epam.training.workload.messaging.WorkloadMessageListener;
import com.epam.training.workload.support.TestTokens;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.springframework.jms.core.JmsTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WorkloadMessagingSteps {

    private static final String QUEUE = "trainer.workload";
    private static final String DLQ = "DLQ.trainer.workload";

    private final JmsTemplate jmsTemplate;
    private Message deadLetter;

    public WorkloadMessagingSteps(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @Before
    public void drainDeadLetterQueue() {
        jmsTemplate.setReceiveTimeout(100);
        while (jmsTemplate.receive(DLQ) != null) {
        }
    }

    private static String body(String action, String username, String date, int minutes) {
        return """
                {"trainerUsername":"%s","trainerFirstName":"Anna","trainerLastName":"Lee","isActive":true,
                 "trainingDate":"%s","trainingDuration":%d,"actionType":"%s"}""".formatted(username, date, minutes,
                action);
    }

    private void send(String body, String token) {
        jmsTemplate.convertAndSend(QUEUE, body, message -> {
            if (token != null) {
                message.setStringProperty(WorkloadMessageListener.TOKEN_PROPERTY, token);
            }
            message.setStringProperty(WorkloadMessageListener.TRANSACTION_ID_PROPERTY, "tx-component");
            return message;
        });
    }

    @When("a {string} message for trainer {string} on {string} for {int} minutes is received with a valid token")
    public void aMessageIsReceivedWithAValidToken(String action, String username, String date, int minutes) {
        send(body(action, username, date, minutes), TestTokens.valid());
    }

    @When("a {string} message for trainer {string} on {string} for {int} minutes is received without a token")
    public void aMessageIsReceivedWithoutAToken(String action, String username, String date, int minutes) {
        send(body(action, username, date, minutes), null);
    }

    @When("a {string} message for trainer {string} on {string} for {int} minutes is received with an expired token")
    public void aMessageIsReceivedWithAnExpiredToken(String action, String username, String date, int minutes) {
        String expired = TestTokens.token(TestTokens.SECRET, "gym-crm", "workload",
                Instant.now().minus(1, ChronoUnit.MINUTES));
        send(body(action, username, date, minutes), expired);
    }

    @When("a message with a valid token and this body is received:")
    public void aMessageWithThisBodyIsReceived(String body) {
        send(body, TestTokens.valid());
    }

    @Then("the message is moved to the dead letter queue with a reason starting with {string}")
    public void theMessageIsMovedToTheDeadLetterQueue(String reason) throws JMSException {
        jmsTemplate.setReceiveTimeout(5000);
        deadLetter = jmsTemplate.receive(DLQ);
        assertNotNull(deadLetter, "expected a message on " + DLQ);
        String actual = deadLetter.getStringProperty(DeadLetterPublisher.REASON_PROPERTY);
        assertTrue(actual.startsWith(reason), "unexpected reason: " + actual);
    }

    @Then("the dead letter keeps the original queue and transaction id")
    public void theDeadLetterKeepsTheOriginalQueue() throws JMSException {
        assertEquals(QUEUE, deadLetter.getStringProperty(DeadLetterPublisher.ORIGINAL_QUEUE_PROPERTY));
        assertEquals("tx-component", deadLetter.getStringProperty(WorkloadMessageListener.TRANSACTION_ID_PROPERTY));
    }
}
