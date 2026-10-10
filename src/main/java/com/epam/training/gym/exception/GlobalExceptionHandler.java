package com.epam.training.gym.exception;

import com.epam.training.gym.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * One place that turns every exception into the same JSON shape
 * ({@link ErrorResponse}) with the right HTTP status (task note 15/18:
 * "Implement error handling for all endpoints").
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException e) {
        return respond(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        return respond(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return respond(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * Anything unexpected. The real cause is logged (with stack trace) for
     * the developer, but the client only gets a generic message - never
     * leak internal details through the REST response.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unhandled exception, transactionId={}", MDC.get("transactionId"), e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error, please try again later");
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String message) {
        String transactionId = MDC.get("transactionId");
        log.warn("Request failed: status={} message={} transactionId={}", status.value(), message, transactionId);
        return ResponseEntity.status(status).body(new ErrorResponse(message, status.value(), transactionId));
    }
}