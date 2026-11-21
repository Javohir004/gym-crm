package com.epam.training.workload.messaging;

import com.epam.training.workload.logging.TransactionIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class DeadLetterPublisher {

    public static final String REASON_PROPERTY = "errorReason";
    public static final String ORIGINAL_QUEUE_PROPERTY = "originalQueue";

    private static final Logger log = LoggerFactory.getLogger(DeadLetterPublisher.class);

    private final JmsTemplate jmsTemplate;
    private final String deadLetterQueue;
    private final String originalQueue;

    public DeadLetterPublisher(JmsTemplate jmsTemplate,
                               @Value("${workload.jms.dead-letter-queue}") String deadLetterQueue,
                               @Value("${workload.jms.queue}") String originalQueue) {
        this.jmsTemplate = jmsTemplate;
        this.deadLetterQueue = deadLetterQueue;
        this.originalQueue = originalQueue;
    }

    public void send(String body, String reason) {
        String transactionId = MDC.get(TransactionIdFilter.MDC_KEY);
        jmsTemplate.convertAndSend(deadLetterQueue, body, message -> {
            message.setStringProperty(REASON_PROPERTY, reason);
            message.setStringProperty(ORIGINAL_QUEUE_PROPERTY, originalQueue);
            if (transactionId != null) {
                message.setStringProperty(WorkloadMessageListener.TRANSACTION_ID_PROPERTY, transactionId);
            }
            return message;
        });
        log.error("Message moved to dead letter queue '{}': {}", deadLetterQueue, reason);
    }
}
