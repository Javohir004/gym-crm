package logging;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.logging.RestCallLoggingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import support.LogCapture;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestCallLoggingFilterTest {

    private final RestCallLoggingFilter filter = new RestCallLoggingFilter();

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @Test
    void doFilter_logsTheIncomingCall_andTheFinalStatus() throws IOException, ServletException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/training-types");
        when(response.getStatus()).thenReturn(200);

        try (LogCapture logs = new LogCapture(RestCallLoggingFilter.class)) {
            filter.doFilter(request, response, chain);

            assertTrue(logs.messages(Level.INFO).stream()
                    .anyMatch(m -> m.contains("GET") && m.contains("/api/v1/training-types") && m.contains("received")));
            assertTrue(logs.messages(Level.INFO).stream()
                    .anyMatch(m -> m.contains("status=200")));
        }
    }

    @Test
    void doFilter_alwaysCallsTheRestOfTheChain() throws IOException, ServletException {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/trainees");
        when(response.getStatus()).thenReturn(201);

        filter.doFilter(request, response, chain);

        org.mockito.Mockito.verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_logsTheErrorStatus_whenTheCallFails() throws IOException, ServletException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/trainees/John.Doe");
        when(response.getStatus()).thenReturn(401);

        try (LogCapture logs = new LogCapture(RestCallLoggingFilter.class)) {
            filter.doFilter(request, response, chain);

            assertTrue(logs.messages(Level.INFO).stream().anyMatch(m -> m.contains("status=401")));
        }
    }
}