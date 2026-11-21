package com.epam.training.workload.exception;

import com.epam.training.workload.dto.ErrorResponse;
import com.epam.training.workload.logging.TransactionIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.DateTimeException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(DateTimeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDate(DateTimeException e) {
        return respond(HttpStatus.BAD_REQUEST, "Invalid month or date");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleStorageFailure(DataAccessException e) {
        log.error("Workload storage failure", e);
        return respond(HttpStatus.SERVICE_UNAVAILABLE, "Workload storage is temporarily unavailable");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error, please try again later");
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        ErrorResponse error = errorResponse(statusCode.value(), clientMessage(ex, body, statusCode));
        return ResponseEntity.status(statusCode).headers(headers).body(error);
    }

    private String clientMessage(Exception ex, Object body, HttpStatusCode statusCode) {
        if (ex instanceof MethodArgumentNotValidException e) {
            return e.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + " " + error.getDefaultMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return "Malformed request body";
        }
        if (body instanceof ProblemDetail problem && problem.getDetail() != null) {
            return problem.getDetail();
        }
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status != null ? status.getReasonPhrase() : "Request failed";
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(errorResponse(status.value(), message));
    }

    private ErrorResponse errorResponse(int status, String message) {
        log.warn("Request failed: status={} message={}", status, message);
        return new ErrorResponse(message, status, MDC.get(TransactionIdFilter.MDC_KEY));
    }
}
