package controller;

import com.epam.training.gym.controller.AuthController;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.exception.GlobalExceptionHandler;
import com.epam.training.gym.service.AuthenticationService;
import com.epam.training.gym.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import support.BasicAuthHeader;

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
    void login_returns200_whenHeaderCredentialsMatch() throws Exception {
        mockMvc.perform(get("/api/v1/login")
                        .header(HttpHeaders.AUTHORIZATION, BasicAuthHeader.value("John.Doe", "pass123456")))
                .andExpect(status().isOk());

        verify(authenticationService).authenticate("John.Doe", "pass123456");
    }

    @Test
    void login_returns401_whenCredentialsDoNotMatch() throws Exception {
        when(authenticationService.authenticate("John.Doe", "wrong"))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        mockMvc.perform(get("/api/v1/login")
                        .header(HttpHeaders.AUTHORIZATION, BasicAuthHeader.value("John.Doe", "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_ignoresCredentialsInQueryParameters() throws Exception {
        mockMvc.perform(get("/api/v1/login").param("username", "John.Doe").param("password", "pass123456"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(authenticationService);
    }

    @Test
    void changeLogin_returns200_andDelegatesToUserAccountService() throws Exception {
        String body = """
                {"username":"John.Doe","oldPassword":"oldPass123","newPassword":"newPass123"}
                """;

        mockMvc.perform(put("/api/v1/login").contentType("application/json").content(body))
                .andExpect(status().isOk());

        verify(userAccountService).changePassword("John.Doe", "oldPass123", "newPass123");
    }
}
