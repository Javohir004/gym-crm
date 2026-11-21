package security;

import com.epam.training.gym.security.BearerToken;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BearerTokenTest {

    private MockHttpServletRequest requestWith(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }

    @Test
    void from_returnsTheToken() {
        assertEquals("abc.def.ghi", BearerToken.from(requestWith("Bearer abc.def.ghi")).orElseThrow());
    }

    @Test
    void from_isEmpty_whenHeaderIsMissing() {
        assertTrue(BearerToken.from(requestWith(null)).isEmpty());
    }

    @Test
    void from_isEmpty_forOtherSchemes() {
        assertTrue(BearerToken.from(requestWith("Basic dXNlcjpwYXNz")).isEmpty());
    }

    @Test
    void from_isEmpty_whenTokenIsBlank() {
        assertTrue(BearerToken.from(requestWith("Bearer   ")).isEmpty());
    }
}