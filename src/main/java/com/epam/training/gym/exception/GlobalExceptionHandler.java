package com.epam.training.gym.exception;

import com.epam.training.gym.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;


/**
 * Our own exceptions are mapped by the handlers below; Spring MVC's exceptions (missing parameter,
 * malformed body, wrong method, unsupported media type, unknown URL, ...) get their correct 4xx
 * status from {@link ResponseEntityExceptionHandler} and are rendered in the same {@link ErrorResponse} shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

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

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {
        return respond(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return respond(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unhandled exception, transactionId={}", MDC.get("transactionId"), e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error, please try again later");
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        ErrorResponse error = errorResponse(statusCode.value(), clientMessage(ex, body, statusCode));
        return ResponseEntity.status(statusCode).headers(headers).body(error);
    }

    private String clientMessage(Exception ex, Object body, HttpStatusCode statusCode) {
        if (ex instanceof MissingServletRequestParameterException e) {
            return "Missing required parameter: " + e.getParameterName();
        }
        if (ex instanceof MethodArgumentTypeMismatchException e) {
            return "Invalid value for parameter: " + e.getName();
        }
        if (ex instanceof TypeMismatchException e) {
            return "Invalid value for parameter: " + e.getPropertyName();
        }
        if (ex instanceof HttpMessageNotReadableException) {
            // the parser message can expose internal class names, so it is not passed on
            return "Malformed request body";
        }
        if (ex instanceof NoResourceFoundException) {
            return "Resource not found";
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
        String transactionId = MDC.get("transactionId");
        log.warn("Request failed: status={} message={} transactionId={}", status, message, transactionId);
        return new ErrorResponse(message, status, transactionId);
    }
}
