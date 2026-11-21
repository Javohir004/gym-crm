package controller;

import com.epam.training.gym.controller.TrainingController;
import com.epam.training.gym.exception.GlobalExceptionHandler;
import com.epam.training.gym.exception.NotFoundException;
import com.epam.training.gym.service.TrainingService;
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

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TrainingControllerTest {

    private static final String BODY = """
            {"traineeUsername":"John.Doe","trainerUsername":"Anna.Lee","trainingName":"Morning Yoga",
             "trainingDate":"2026-09-01","trainingDuration":60}
            """;

    @Mock
    private TrainingService trainingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(new TrainingController(trainingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    @Test
    void addTraining_returns200_andDelegatesToService() throws Exception {
        mockMvc.perform(post("/api/trainings").contentType("application/json").content(BODY))
                .andExpect(status().isOk());

        verify(trainingService).addTraining("John.Doe", "Anna.Lee", "Morning Yoga", LocalDate.of(2026, 9, 1), 60);
    }

    @Test
    void addTraining_returns404_whenTrainerDoesNotExist() throws Exception {
        when(trainingService.addTraining("John.Doe", "Anna.Lee", "Morning Yoga", LocalDate.of(2026, 9, 1), 60))
                .thenThrow(new NotFoundException("Trainer not found: Anna.Lee"));

        mockMvc.perform(post("/api/trainings").contentType("application/json").content(BODY))
                .andExpect(status().isNotFound());
    }
}