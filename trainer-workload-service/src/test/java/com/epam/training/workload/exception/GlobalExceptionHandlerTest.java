package com.epam.training.workload.exception;

import com.epam.training.workload.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void storageFailure_returns503_withoutLeakingDriverDetails() {
        ResponseEntity<ErrorResponse> response = handler.handleStorageFailure(
                new DataAccessResourceFailureException("Timed out connecting to mongodb://admin:secret@db:27017"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Workload storage is temporarily unavailable", response.getBody().message());
        assertFalse(response.getBody().message().contains("secret"));
    }

    @Test
    void unexpectedError_returns500_withAGenericMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new IllegalStateException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().message().contains("boom"));
    }
}
