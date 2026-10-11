package controller;

import com.epam.training.gym.controller.AuthController;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.exception.GlobalExceptionHandler;
import com.epam.training.gym.facade.GymFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private GymFacade facade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(facade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_returns200_whenCredentialsMatch() throws Exception {
        mockMvc.perform(get("/api/login").param("username", "John.Doe").param("password", "pass123456"))
                .andExpect(status().isOk());

        verify(facade).login("John.Doe", "pass123456");
    }

    @Test
    void login_returns401_whenCredentialsDoNotMatch() throws Exception {
        doThrow(new AuthenticationException("Invalid username or password"))
                .when(facade).login("John.Doe", "wrong");

        mockMvc.perform(get("/api/login").param("username", "John.Doe").param("password", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void changeLogin_returns200_andDelegatesToFacade() throws Exception {
        String body = """
                {"username":"John.Doe","oldPassword":"oldPass123","newPassword":"newPass123"}
                """;

        mockMvc.perform(put("/api/login").contentType("application/json").content(body))
                .andExpect(status().isOk());

        verify(facade).changeLogin("John.Doe", "oldPass123", "newPass123");
    }
}