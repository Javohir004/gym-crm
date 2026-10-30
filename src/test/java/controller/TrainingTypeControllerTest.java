package controller;

import com.epam.training.gym.controller.TrainingTypeController;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.exception.GlobalExceptionHandler;
import com.epam.training.gym.model.TrainingType;
import com.epam.training.gym.service.AuthenticationService;
import com.epam.training.gym.service.TrainingTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import support.BasicAuthHeader;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TrainingTypeControllerTest {

    private static final String USER = "John.Doe";
    private static final String PASS = "secret1234";

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private TrainingTypeService trainingTypeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TrainingTypeController(authenticationService, trainingTypeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAll_withValidCredentials_returns200WithList() throws Exception {
        when(trainingTypeService.getAll()).thenReturn(List.of(
                TrainingType.builder().id(1L).trainingTypeName("Fitness").build(),
                TrainingType.builder().id(2L).trainingTypeName("Yoga").build()));

        mockMvc.perform(get("/api/training-types")
                        .header(HttpHeaders.AUTHORIZATION, BasicAuthHeader.value(USER, PASS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingType").value("Fitness"))
                .andExpect(jsonPath("$[0].trainingTypeId").value(1))
                .andExpect(jsonPath("$[1].trainingType").value("Yoga"));
    }

    @Test
    void getAll_withoutAuthorizationHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/training-types"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(authenticationService, trainingTypeService);
    }

    @Test
    void getAll_withWrongPassword_returns401() throws Exception {
        when(authenticationService.authenticate(USER, "wrong"))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        mockMvc.perform(get("/api/training-types")
                        .header(HttpHeaders.AUTHORIZATION, BasicAuthHeader.value(USER, "wrong")))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(trainingTypeService);
    }
}
