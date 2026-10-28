package security;

import com.epam.training.gym.security.JwtAuthenticationFilter;
import com.epam.training.gym.security.JwtService;
import com.epam.training.gym.security.TokenBlacklist;
import com.epam.training.gym.security.TokenClaims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private TokenBlacklist tokenBlacklist;
    @Mock
    private FilterChain chain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtService, tokenBlacklist);
        request = new MockHttpServletRequest("GET", "/api/trainees/John.Doe");
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validToken_authenticatesTheRequestAsTheTokenOwner() throws Exception {
        request.addHeader("Authorization", "Bearer good-token");
        when(jwtService.parse("good-token"))
                .thenReturn(Optional.of(new TokenClaims("id-1", "John.Doe", Instant.now())));
        when(tokenBlacklist.isRevoked("id-1")).thenReturn(false);

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("John.Doe", authentication.getName());
        assertTrue(authentication.isAuthenticated());
        verify(chain).doFilter(request, response);
    }

    @Test
    void noHeader_leavesRequestAnonymous_andContinuesTheChain() throws Exception {
        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidToken_leavesRequestAnonymous() throws Exception {
        request.addHeader("Authorization", "Bearer bad-token");
        when(jwtService.parse("bad-token")).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void revokedToken_leavesRequestAnonymous() throws Exception {
        request.addHeader("Authorization", "Bearer logged-out-token");
        when(jwtService.parse("logged-out-token"))
                .thenReturn(Optional.of(new TokenClaims("id-2", "John.Doe", Instant.now())));
        when(tokenBlacklist.isRevoked("id-2")).thenReturn(true);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
}