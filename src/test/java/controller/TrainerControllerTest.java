package controller;

import com.epam.training.gym.controller.TrainerController;
import com.epam.training.gym.exception.GlobalExceptionHandler;
import com.epam.training.gym.service.TrainerService;
import com.epam.training.gym.service.TrainingService;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import com.epam.training.gym.model.TrainingType;
import com.epam.training.gym.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import support.BasicAuthHeader;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TrainerControllerTest {

    private static final String USERNAME = "Anna.Lee";
    private static final String PASSWORD = "pass123456";
    private static final String AUTH_HEADER = BasicAuthHeader.value(USERNAME, PASSWORD);

    @Mock
    private TrainerService trainerService;

    @Mock
    private TrainingService trainingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TrainerController(trainerService, trainingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static Trainer trainer(boolean active, TrainingType specialization) {
        User user = User.builder().firstName("Anna").lastName("Lee").username(USERNAME)
                .password(PASSWORD).active(active).build();
        return Trainer.builder().user(user).specialization(specialization).trainees(Set.of()).build();
    }

    @Test
    void register_returns201WithGeneratedCredentials() throws Exception {
        when(trainerService.createTrainer("Anna", "Lee", "Yoga"))
                .thenReturn(trainer(true, TrainingType.builder().trainingTypeName("Yoga").build()));

        String body = """
                {"firstName":"Anna","lastName":"Lee","specialization":"Yoga"}
                """;

        mockMvc.perform(post("/api/v1/trainers").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.password").value(PASSWORD));
    }

    @Test
    void getProfile_returns200WithTraineesList() throws Exception {
        TrainingType yoga = TrainingType.builder().trainingTypeName("Yoga").build();
        Trainer trainer = trainer(true, yoga);
        User traineeUser = User.builder().firstName("John").lastName("Doe").username("John.Doe").build();
        trainer.setTrainees(Set.of(Trainee.builder().user(traineeUser).build()));
        when(trainerService.getByUsername(USERNAME, PASSWORD)).thenReturn(trainer);

        mockMvc.perform(get("/api/v1/trainers/" + USERNAME).header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialization").value("Yoga"))
                .andExpect(jsonPath("$.trainees[0].username").value("John.Doe"));
    }

    @Test
    void updateProfile_returns200_andSpecializationStaysWhatServiceReturns() throws Exception {
        TrainingType yoga = TrainingType.builder().trainingTypeName("Yoga").build();
        when(trainerService.updateFullProfile(USERNAME, PASSWORD, "Anna", "Lee", true))
                .thenReturn(trainer(true, yoga));

        // Even though the request carries "Fitness", the controller must not pass it to the
        // service as a value to apply - specialization is read-only (task note under endpoint 9).
        String body = """
                {"username":"Anna.Lee","firstName":"Anna","lastName":"Lee","specialization":"Fitness","isActive":true}
                """;

        mockMvc.perform(put("/api/v1/trainers/" + USERNAME).header("Authorization", AUTH_HEADER)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialization").value("Yoga"));
    }

    @Test
    void updateProfile_returns400_whenIsActiveMissing() throws Exception {
        String body = """
                {"username":"Anna.Lee","firstName":"Anna","lastName":"Lee"}
                """;

        mockMvc.perform(put("/api/v1/trainers/" + USERNAME).header("Authorization", AUTH_HEADER)
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTrainings_returns200WithList() throws Exception {
        Training training = Training.builder().trainingName("Morning Yoga")
                .trainingDate(LocalDate.of(2026, 9, 1)).trainingDuration(60)
                .trainingType(TrainingType.builder().trainingTypeName("Yoga").build())
                .trainee(Trainee.builder().user(User.builder().firstName("John").lastName("Doe").build()).build())
                .build();
        when(trainingService.getTrainerTrainings(USERNAME, PASSWORD, null, null, null)).thenReturn(List.of(training));

        mockMvc.perform(get("/api/v1/trainers/" + USERNAME + "/trainings").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].traineeName").value("John Doe"));
    }

    @Test
    void setActive_returns200() throws Exception {
        String body = """
                {"username":"Anna.Lee","isActive":false}
                """;

        mockMvc.perform(patch("/api/v1/trainers/" + USERNAME + "/status").header("Authorization", AUTH_HEADER)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk());
    }
}