package logging;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.logging.RestCallLoggingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import support.LogCapture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestCallLoggingFilterTest {

    private final RestCallLoggingFilter filter = new RestCallLoggingFilter();

    private static MockHttpServletRequest jsonRequest(String method, String uri, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private static FilterChain respondingWith(int status, String body) {
        return (req, res) -> {
            req.getInputStream().readAllBytes();
            ((HttpServletResponse) res).setStatus(status);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write(body);
        };
    }

    @Test
    void logsEndpointRequestStatusAndResponse() throws ServletException, IOException {
        MockHttpServletRequest request = jsonRequest("POST", "/api/trainings", "{\"trainingName\":\"Yoga\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (LogCapture logs = new LogCapture(RestCallLoggingFilter.class)) {
            filter.doFilter(request, response, respondingWith(200, "{\"result\":\"ok\"}"));

            assertTrue(logs.messages(Level.INFO).stream()
                    .anyMatch(m -> m.contains("received") && m.contains("POST /api/trainings")));
            assertTrue(logs.messages(Level.INFO).stream().anyMatch(m -> m.contains("request={\"trainingName\":\"Yoga\"}")
                    && m.contains("status=200") && m.contains("response={\"result\":\"ok\"}")));
        }
    }

    @Test
    void responseBody_stillReachesTheClient() throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(jsonRequest("GET", "/api/trainees/John.Doe", ""), response,
                respondingWith(200, "{\"firstName\":\"John\"}"));

        assertEquals("{\"firstName\":\"John\"}", response.getContentAsString());
    }

    @Test
    void passwordsAndTokens_neverReachTheLog() throws ServletException, IOException {
        MockHttpServletRequest request = jsonRequest("POST", "/api/login",
                "{\"username\":\"John.Doe\",\"password\":\"secret123\"}");

        try (LogCapture logs = new LogCapture(RestCallLoggingFilter.class)) {
            filter.doFilter(request, new MockHttpServletResponse(),
                    respondingWith(200, "{\"token\":\"eyJhbGciOiJIUzM4NCJ9.payload.signature\"}"));

            assertTrue(logs.allMessages().stream().noneMatch(m -> m.contains("secret123") || m.contains("eyJhbGci")));
            assertTrue(logs.allMessages().stream().anyMatch(m -> m.contains("John.Doe")));
        }
    }

    @Test
    void errorResponse_isLoggedAsWarningWithItsMessage() throws ServletException, IOException {
        try (LogCapture logs = new LogCapture(RestCallLoggingFilter.class)) {
            filter.doFilter(jsonRequest("GET", "/api/trainees/John.Doe", ""), new MockHttpServletResponse(),
                    respondingWith(404, "{\"message\":\"Trainee not found\"}"));

            assertTrue(logs.messages(Level.WARN).stream()
                    .anyMatch(m -> m.contains("status=404") && m.contains("Trainee not found")));
        }
    }

    @Test
    void nonJsonBodies_areSummarised_notDumped() throws ServletException, IOException {
        try (LogCapture logs = new LogCapture(RestCallLoggingFilter.class)) {
            filter.doFilter(new MockHttpServletRequest("GET", "/actuator/prometheus"), new MockHttpServletResponse(),
                    (req, res) -> {
                        res.setContentType("text/plain");
                        res.getWriter().write("gym_registrations_total 5");
                    });

            assertTrue(logs.messages(Level.INFO).stream()
                    .anyMatch(m -> m.contains("request=<empty>") && m.contains("bytes of text/plain")));
        }
    }

    @Test
    void longBodies_areTruncated() throws ServletException, IOException {
        String longName = "x".repeat(2000);

        try (LogCapture logs = new LogCapture(RestCallLoggingFilter.class)) {
            filter.doFilter(jsonRequest("POST", "/api/trainees", "{\"firstName\":\"" + longName + "\"}"),
                    new MockHttpServletResponse(), respondingWith(201, "{}"));

            assertTrue(logs.messages(Level.INFO).stream().anyMatch(m -> m.contains("...(truncated)")));
        }
    }
}
