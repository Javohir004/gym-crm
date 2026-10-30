package exception;


import com.epam.training.gym.dto.ErrorResponse;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.exception.ConflictException;
import com.epam.training.gym.exception.GlobalExceptionHandler;
import com.epam.training.gym.exception.NotFoundException;
import com.epam.training.gym.exception.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;


class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void setTransactionId() {
        MDC.put("transactionId", "test-tx-id");
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void validationException_mapsTo400() {
        ResponseEntity<ErrorResponse> response = handler.handleValidation(new ValidationException("First name required"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("First name required", response.getBody().message());
        assertEquals(400, response.getBody().status());
        assertEquals("test-tx-id", response.getBody().transactionId());
    }

    @Test
    void authenticationException_mapsTo401() {
        ResponseEntity<ErrorResponse> response = handler.handleAuthentication(new AuthenticationException("Invalid username or password"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().status());
    }

    @Test
    void notFoundException_mapsTo404() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(new NotFoundException("Trainee not found: John.Doe"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
    }

    @Test
    void conflictException_mapsTo409() {
        ResponseEntity<ErrorResponse> response = handler.handleConflict(new ConflictException("User is already active"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().status());
    }

    @Test
    void illegalArgumentException_mapsTo400() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(new IllegalArgumentException("Bad input"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void unexpectedException_mapsTo500_andNeverLeaksTheRealMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new RuntimeException("db password is hunter2"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().status());
        assertFalse(response.getBody().message().contains("hunter2"));
    }

    @Test
    void unknownResource_returns404_notServerError() {
        var response = handler.handleNoResource(new NoResourceFoundException(HttpMethod.GET, "/favicon.ico"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void missingRequestParameter_returns400_notServerError() {
        var response = handler.handleMissingParameter(new MissingServletRequestParameterException("username", "String"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Missing required parameter: username", response.getBody().message());
    }

    @Test
    void parameterTypeMismatch_returns400_notServerError() {
        var exception = new MethodArgumentTypeMismatchException("abc", LocalDate.class, "periodFrom", null, null);

        var response = handler.handleTypeMismatch(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid value for parameter: periodFrom", response.getBody().message());
    }

    @Test
    void malformedBody_returns400_withoutParserDetails() {
        var exception = new HttpMessageNotReadableException("JSON parse error at line 1",
                new MockHttpInputMessage(new byte[0]));

        var response = handler.handleUnreadableBody(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed request body", response.getBody().message());
    }

    @Test
    void wrongHttpMethod_returns405_notServerError() {
        var response = handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("POST"));

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}