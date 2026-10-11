package controller;

import com.epam.training.gym.controller.TrainingController;
import com.epam.training.gym.exception.GlobalExceptionHandler;
import com.epam.training.gym.exception.NotFoundException;
import com.epam.training.gym.facade.GymFacade;
import com.epam.training.gym.model.Training;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import support.BasicAuthHeader;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TrainingControllerTest {

    private static final String USERNAME = "John.Doe";
    private static final String PASSWORD = "pass123456";
    private static final String AUTH_HEADER = BasicAuthHeader.value(USERNAME, PASSWORD);

    @Mock
    private GymFacade facade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(new TrainingController(facade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    @Test
    void addTraining_returns200_andDelegatesToFacade_withNoTrainingTypeField() throws Exception {
        when(facade.addTraining(USERNAME, PASSWORD, "John.Doe", "Anna.Lee", "Morning Yoga",
                LocalDate.of(2026, 9, 1), 60)).thenReturn(Training.builder().build());

        String body = """
                {"traineeUsername":"John.Doe","trainerUsername":"Anna.Lee",
                 "trainingName":"Morning Yoga","trainingDate":"2026-09-01","trainingDuration":60}
                """;

        mockMvc.perform(post("/api/trainings").header("Authorization", AUTH_HEADER)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk());

        verify(facade).addTraining(USERNAME, PASSWORD, "John.Doe", "Anna.Lee", "Morning Yoga",
                LocalDate.of(2026, 9, 1), 60);
    }

    @Test
    void addTraining_returns404_whenTrainerDoesNotExist() throws Exception {
        when(facade.addTraining(USERNAME, PASSWORD, "John.Doe", "No.Such", "Morning Yoga",
                LocalDate.of(2026, 9, 1), 60)).thenThrow(new NotFoundException("Trainer not found: No.Such"));

        String body = """
                {"traineeUsername":"John.Doe","trainerUsername":"No.Such",
                 "trainingName":"Morning Yoga","trainingDate":"2026-09-01","trainingDuration":60}
                """;

        mockMvc.perform(post("/api/trainings").header("Authorization", AUTH_HEADER)
                        .contentType("application/json").content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void addTraining_returns401_whenMissingAuthorizationHeader() throws Exception {
        String body = """
                {"traineeUsername":"John.Doe","trainerUsername":"Anna.Lee",
                 "trainingName":"Morning Yoga","trainingDate":"2026-09-01","trainingDuration":60}
                """;

        mockMvc.perform(post("/api/trainings").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
    }
}