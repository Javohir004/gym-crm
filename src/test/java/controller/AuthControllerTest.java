package controller;

import com.epam.training.gym.controller.AuthController;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.exception.GlobalExceptionHandler;
import com.epam.training.gym.model.User;
import com.epam.training.gym.service.AuthenticationService;
import com.epam.training.gym.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

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
    void login_returns200_whenCredentialsMatch() throws Exception {
        mockMvc.perform(get("/api/v1/login").param("username", "John.Doe").param("password", "pass123456"))
                .andExpect(status().isOk());

        verify(authenticationService).authenticate("John.Doe", "pass123456");
    }

    @Test
    void login_returns401_whenCredentialsDoNotMatch() throws Exception {
        doThrow(new AuthenticationException("Invalid username or password"))
                .when(authenticationService).authenticate("John.Doe", "wrong");

        mockMvc.perform(get("/api/v1/login").param("username", "John.Doe").param("password", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void changeLogin_returns200_andChangesPassword() throws Exception {
        User user = User.builder().username("John.Doe").password("oldPass123").build();
        when(authenticationService.authenticate("John.Doe", "oldPass123")).thenReturn(user);
        String body = """
                {"username":"John.Doe","oldPassword":"oldPass123","newPassword":"newPass123"}
                """;

        mockMvc.perform(put("/api/v1/login").contentType("application/json").content(body))
                .andExpect(status().isOk());

        verify(userAccountService).changePassword(user, "newPass123");
    }

    @Test
    void changeLogin_returns401_andDoesNotChangePassword_whenOldPasswordIsWrong() throws Exception {
        when(authenticationService.authenticate("John.Doe", "wrong"))
                .thenThrow(new AuthenticationException("Invalid username or password"));
        String body = """
                {"username":"John.Doe","oldPassword":"wrong","newPassword":"newPass123"}
                """;

        mockMvc.perform(put("/api/v1/login").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userAccountService);
    }
}