package controller;

import com.epam.training.gym.controller.AuthController;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.exception.GlobalExceptionHandler;
import com.epam.training.gym.exception.TooManyAttemptsException;
import com.epam.training.gym.service.AuthenticationService;
import com.epam.training.gym.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final Principal JOHN = () -> "John.Doe";
    private static final String LOGIN_BODY = """
            {"username":"John.Doe","password":"pass123456"}
            """;
    private static final String CHANGE_BODY = """
            {"username":"John.Doe","oldPassword":"oldPass123","newPassword":"newPass123"}
            """;

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private UserAccountService userAccountService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authenticationService, userAccountService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_returns200WithToken_whenCredentialsMatch() throws Exception {
        when(authenticationService.login("John.Doe", "pass123456")).thenReturn("jwt-token");

        mockMvc.perform(post("/api/login").contentType("application/json").content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_returns401_whenCredentialsDoNotMatch() throws Exception {
        when(authenticationService.login("John.Doe", "wrong")).thenThrow(new AuthenticationException("Invalid username or password"));

        mockMvc.perform(post("/api/login").contentType("application/json")
                        .content("{\"username\":\"John.Doe\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_returns429_whenUserIsBlocked() throws Exception {
        when(authenticationService.login("John.Doe", "pass123456"))
                .thenThrow(new TooManyAttemptsException("Too many failed login attempts. Try again in 5 minute(s)"));

        mockMvc.perform(post("/api/login").contentType("application/json").content(LOGIN_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void changeLogin_returns200_andDelegatesToService() throws Exception {
        mockMvc.perform(put("/api/login").principal(JOHN).contentType("application/json").content(CHANGE_BODY))
                .andExpect(status().isOk());

        verify(userAccountService).changePassword("John.Doe", "oldPass123", "newPass123");
    }

    @Test
    void changeLogin_returns403_whenChangingSomeoneElsesPassword() throws Exception {
        String body = """
                {"username":"Anna.Lee","oldPassword":"oldPass123","newPassword":"newPass123"}
                """;

        mockMvc.perform(put("/api/login").principal(JOHN).contentType("application/json").content(body))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userAccountService);
    }

    @Test
    void changeLogin_returns401_whenNobodyIsAuthenticated() throws Exception {
        mockMvc.perform(put("/api/login").contentType("application/json").content(CHANGE_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userAccountService);
    }

    @Test
    void changeLogin_returns401_whenOldPasswordIsWrong() throws Exception {
        doThrow(new AuthenticationException("Invalid username or old password"))
                .when(userAccountService).changePassword("John.Doe", "oldPass123", "newPass123");

        mockMvc.perform(put("/api/login").principal(JOHN).contentType("application/json").content(CHANGE_BODY))
                .andExpect(status().isUnauthorized());
    }
}