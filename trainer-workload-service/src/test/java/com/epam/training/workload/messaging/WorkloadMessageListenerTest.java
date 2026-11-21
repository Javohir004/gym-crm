package com.epam.training.workload.messaging;

import com.epam.training.workload.dto.WorkloadRequest;
import com.epam.training.workload.model.ActionType;
import com.epam.training.workload.security.ServiceTokenValidator;
import com.epam.training.workload.service.TrainerWorkloadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkloadMessageListenerTest {

    private static final ValidatorFactory VALIDATION = Validation.buildDefaultValidatorFactory();
    private static final String VALID_BODY = """
            {"trainerUsername":"Anna.Lee","trainerFirstName":"Anna","trainerLastName":"Lee","isActive":true,
             "trainingDate":"2026-03-01","trainingDuration":60,"actionType":"ADD"}""";
    private static final WorkloadRequest VALID_REQUEST = new WorkloadRequest("Anna.Lee", "Anna", "Lee", true,
            LocalDate.of(2026, 3, 1), 60, ActionType.ADD);

    @Mock
    private TrainerWorkloadService service;
    @Mock
    private ServiceTokenValidator tokenValidator;
    @Mock
    private DeadLetterPublisher deadLetters;

    private WorkloadMessageListener listener;

    @AfterAll
    static void closeValidation() {
        VALIDATION.close();
    }

    @BeforeEach
    void setUp() {
        Validator validator = VALIDATION.getValidator();
        listener = new WorkloadMessageListener(service, tokenValidator, deadLetters,
                new ObjectMapper().findAndRegisterModules(), validator, 3);
    }

    private void tokenIsValid() {
        when(tokenValidator.validate("good-token")).thenReturn(Optional.of("gym-crm"));
    }

    @Test
    void validMessage_isProcessed() {
        tokenIsValid();

        listener.onMessage(VALID_BODY, "good-token", "tx-1", 1);

        verify(service).process(VALID_REQUEST);
        verifyNoInteractions(deadLetters);
    }

    @Test
    void missingToken_goesStraightToTheDeadLetterQueue() {
        listener.onMessage(VALID_BODY, null, "tx-1", 1);

        verify(deadLetters).send(VALID_BODY, "Missing or invalid service token");
        verifyNoInteractions(service);
    }

    @Test
    void invalidToken_goesStraightToTheDeadLetterQueue() {
        when(tokenValidator.validate("forged")).thenReturn(Optional.empty());

        listener.onMessage(VALID_BODY, "forged", "tx-1", 1);

        verify(deadLetters).send(VALID_BODY, "Missing or invalid service token");
        verifyNoInteractions(service);
    }

    @Test
    void malformedJson_goesStraightToTheDeadLetterQueue() {
        tokenIsValid();

        listener.onMessage("{ not json", "good-token", "tx-1", 1);

        verify(deadLetters).send(eq("{ not json"), startsWith("Malformed JSON"));
        verifyNoInteractions(service);
    }

    @Test
    void missingRequiredFields_goStraightToTheDeadLetterQueue_namingThem() {
        tokenIsValid();
        String body = "{\"trainerUsername\":\"Anna.Lee\",\"trainingDuration\":-5}";

        listener.onMessage(body, "good-token", "tx-1", 1);

        verify(deadLetters).send(eq(body), matches(
                "Required information missing or invalid: actionType .*trainingDuration .*"));
        verifyNoInteractions(service);
    }

    @Test
    void processingFailure_beforeTheLastAttempt_isRethrown_soTheBrokerRedelivers() {
        tokenIsValid();
        doThrow(new IllegalStateException("temporary")).when(service).process(any());

        assertThrows(IllegalStateException.class, () -> listener.onMessage(VALID_BODY, "good-token", "tx-1", 2));

        verifyNoInteractions(deadLetters);
    }

    @Test
    void documentValidationFailure_isNotRetried_andGoesToTheDeadLetterQueue() {
        tokenIsValid();
        doThrow(new ConstraintViolationException("trainerUsername must not be blank", Set.of()))
                .when(service).process(any());

        listener.onMessage(VALID_BODY, "good-token", "tx-1", 1);

        verify(deadLetters).send(eq(VALID_BODY), startsWith("Trainer summary would be invalid"));
    }

    @Test
    void processingFailure_onTheLastAttempt_goesToTheDeadLetterQueue() {
        tokenIsValid();
        doThrow(new IllegalStateException("still broken")).when(service).process(any());

        listener.onMessage(VALID_BODY, "good-token", "tx-1", 3);

        verify(deadLetters).send(VALID_BODY, "Failed after 3 attempts: still broken");
    }

    @Test
    void transactionIdIsClearedAfterTheMessage() {
        tokenIsValid();

        listener.onMessage(VALID_BODY, "good-token", "tx-1", 1);

        assertNull(MDC.get("transactionId"));
        verify(deadLetters, never()).send(anyString(), anyString());
    }
}
