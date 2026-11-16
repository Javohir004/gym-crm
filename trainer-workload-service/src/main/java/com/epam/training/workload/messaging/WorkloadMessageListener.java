package com.epam.training.workload.messaging;

import com.epam.training.workload.dto.WorkloadRequest;
import com.epam.training.workload.logging.TransactionIdFilter;
import com.epam.training.workload.security.ServiceTokenValidator;
import com.epam.training.workload.service.TrainerWorkloadService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class WorkloadMessageListener {

    public static final String TOKEN_PROPERTY = "serviceToken";
    public static final String TRANSACTION_ID_PROPERTY = "transactionId";
    static final String DELIVERY_COUNT_PROPERTY = "JMSXDeliveryCount";

    private static final Logger log = LoggerFactory.getLogger(WorkloadMessageListener.class);

    private final TrainerWorkloadService service;
    private final ServiceTokenValidator tokenValidator;
    private final DeadLetterPublisher deadLetters;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final int maxAttempts;

    public WorkloadMessageListener(TrainerWorkloadService service, ServiceTokenValidator tokenValidator,
                                   DeadLetterPublisher deadLetters, ObjectMapper objectMapper, Validator validator,
                                   @Value("${workload.jms.max-attempts}") int maxAttempts) {
        this.service = service;
        this.tokenValidator = tokenValidator;
        this.deadLetters = deadLetters;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.maxAttempts = maxAttempts;
    }

    @JmsListener(destination = "${workload.jms.queue}", concurrency = "${workload.jms.concurrency}")
    public void onMessage(@Payload String body,
                          @Header(name = TOKEN_PROPERTY, required = false) String token,
                          @Header(name = TRANSACTION_ID_PROPERTY, required = false) String transactionId,
                          @Header(name = DELIVERY_COUNT_PROPERTY, defaultValue = "1") int deliveryCount) {
        MDC.put(TransactionIdFilter.MDC_KEY, TransactionIdFilter.resolve(transactionId));
        try {
            log.info("Message received: attempt={} body={}", deliveryCount, body);
            handle(body, token, deliveryCount);
        } finally {
            MDC.remove(TransactionIdFilter.MDC_KEY);
        }
    }

    private void handle(String body, String token, int deliveryCount) {
        if (token == null || tokenValidator.validate(token).isEmpty()) {
            deadLetters.send(body, "Missing or invalid service token");
            return;
        }
        WorkloadRequest request;
        try {
            request = objectMapper.readValue(body, WorkloadRequest.class);
        } catch (JsonProcessingException e) {
            deadLetters.send(body, "Malformed JSON: " + e.getOriginalMessage());
            return;
        }
        Set<ConstraintViolation<WorkloadRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            deadLetters.send(body, "Required information missing or invalid: " + describe(violations));
            return;
        }
        process(body, request, deliveryCount);
    }

    private void process(String body, WorkloadRequest request, int deliveryCount) {
        try {
            service.process(request);
            log.info("Message processed: {} for trainer={}", request.actionType(), request.trainerUsername());
        } catch (RuntimeException e) {
            if (deliveryCount >= maxAttempts) {
                deadLetters.send(body, "Failed after " + deliveryCount + " attempts: " + e.getMessage());
                return;
            }
            log.warn("Attempt {} of {} failed for trainer={}: {}", deliveryCount, maxAttempts,
                    request.trainerUsername(), e.getMessage());
            throw e;
        }
    }

    private static String describe(Set<ConstraintViolation<WorkloadRequest>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
