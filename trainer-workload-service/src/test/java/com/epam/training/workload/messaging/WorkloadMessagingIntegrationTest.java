package com.epam.training.workload.messaging;

import com.epam.training.workload.exception.NotFoundException;
import com.epam.training.workload.service.TrainerWorkloadService;
import com.epam.training.workload.support.TestTokens;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jms.core.JmsTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.Month;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class WorkloadMessagingIntegrationTest {

    private static final String QUEUE = "trainer.workload";
    private static final String DLQ = "DLQ.trainer.workload";
    private static final Pattern USERNAME = Pattern.compile("\"trainerUsername\":\"([^\"]+)\"");

    @Autowired
    private JmsTemplate jmsTemplate;
    @SpyBean
    private TrainerWorkloadService service;

    private static String body(String username, String date, int minutes, String action) {
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
            message.setStringProperty(WorkloadMessageListener.TRANSACTION_ID_PROPERTY, "tx-it");
            Matcher username = USERNAME.matcher(body);
            if (username.find()) {
                message.setStringProperty("JMSXGroupID", username.group(1));
            }
            return message;
        });
    }

    private Message receiveDeadLetter() {
        jmsTemplate.setReceiveTimeout(5000);
        Message message = jmsTemplate.receive(DLQ);
        assertNotNull(message, "expected a message on " + DLQ);
        return message;
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (!condition.getAsBoolean()) {
            if (Instant.now().isAfter(deadline)) {
                fail("condition not met within 5 seconds");
            }
            Thread.sleep(50);
        }
    }

    private long minutes(String username, int year, Month month) {
        try {
            return service.getMonthlyWorkload(username, year, month).trainingSummaryDuration();
        } catch (NotFoundException e) {
            return -1;
        }
    }

    @BeforeEach
    void drainDeadLetterQueue() {
        jmsTemplate.setReceiveTimeout(100);
        while (jmsTemplate.receive(DLQ) != null) {
        }
    }

    @Test
    void addAndDeleteMessages_updateTheTrainersMonthlyWorkload() throws InterruptedException {
        send(body("Queue.Trainer", "2026-05-02", 60, "ADD"), TestTokens.valid());
        send(body("Queue.Trainer", "2026-05-20", 30, "ADD"), TestTokens.valid());
        waitUntil(() -> minutes("Queue.Trainer", 2026, Month.MAY) == 90);

        send(body("Queue.Trainer", "2026-05-20", 30, "DELETE"), TestTokens.valid());
        waitUntil(() -> minutes("Queue.Trainer", 2026, Month.MAY) == 60);
    }

    @Test
    void messageWithMissingFields_endsUpInTheDeadLetterQueue_withTheReason() throws JMSException {
        String invalid = "{\"trainerUsername\":\"Dlq.Trainer\",\"trainingDuration\":60}";

        send(invalid, TestTokens.valid());

        Message dead = receiveDeadLetter();
        assertEquals(invalid, ((TextMessage) dead).getText());
        assertTrue(dead.getStringProperty(DeadLetterPublisher.REASON_PROPERTY)
                .startsWith("Required information missing or invalid: actionType"));
        assertEquals(QUEUE, dead.getStringProperty(DeadLetterPublisher.ORIGINAL_QUEUE_PROPERTY));
        assertEquals("tx-it", dead.getStringProperty(WorkloadMessageListener.TRANSACTION_ID_PROPERTY));
    }

    @Test
    void messageWithoutServiceToken_endsUpInTheDeadLetterQueue() throws JMSException {
        send(body("NoToken.Trainer", "2026-05-02", 60, "ADD"), null);

        assertEquals("Missing or invalid service token",
                receiveDeadLetter().getStringProperty(DeadLetterPublisher.REASON_PROPERTY));
        assertEquals(-1, minutes("NoToken.Trainer", 2026, Month.MAY));
    }

    @Test
    void temporaryFailure_isRetried_andThenSucceeds() throws InterruptedException {
        doThrow(new IllegalStateException("temporary")).doCallRealMethod()
                .when(service).process(argThat(request -> request.trainerUsername().equals("Retry.Trainer")));

        send(body("Retry.Trainer", "2026-06-01", 45, "ADD"), TestTokens.valid());

        waitUntil(() -> minutes("Retry.Trainer", 2026, Month.JUNE) == 45);
        verify(service, times(2)).process(argThat(request -> request.trainerUsername().equals("Retry.Trainer")));
    }

    @Test
    void permanentFailure_isRetriedThreeTimes_thenMovedToTheDeadLetterQueue() throws JMSException {
        doThrow(new IllegalStateException("store unavailable"))
                .when(service).process(argThat(request -> request.trainerUsername().equals("Broken.Trainer")));

        send(body("Broken.Trainer", "2026-06-01", 45, "ADD"), TestTokens.valid());

        assertEquals("Failed after 3 attempts: store unavailable",
                receiveDeadLetter().getStringProperty(DeadLetterPublisher.REASON_PROPERTY));
        verify(service, atLeast(3)).process(argThat(request -> request.trainerUsername().equals("Broken.Trainer")));
        doCallRealMethod().when(service).process(argThat(request -> true));
    }
}
