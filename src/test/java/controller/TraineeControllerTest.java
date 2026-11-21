package controller;

import com.epam.training.gym.controller.TraineeController;
import com.epam.training.gym.exception.GlobalExceptionHandler;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import com.epam.training.gym.model.TrainingType;
import com.epam.training.gym.model.User;
import com.epam.training.gym.service.AuthenticationService;
import com.epam.training.gym.service.Registration;
import com.epam.training.gym.service.TraineeService;
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

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TraineeControllerTest {

    private static final String USERNAME = "John.Doe";
    private static final String PASSWORD = "pass123456";
    private static final Principal PRINCIPAL = () -> USERNAME;

    @Mock
    private TraineeService traineeService;
    @Mock
    private TrainingService trainingService;
    @Mock
    private AuthenticationService authenticationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(new TraineeController(traineeService, trainingService, authenticationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    private static Trainee trainee(boolean active) {
        User user = User.builder().firstName("John").lastName("Doe").username(USERNAME)
                .password(PASSWORD).active(active).build();
        return Trainee.builder().user(user).dateOfBirth(LocalDate.of(2000, 1, 15))
                .address("Tashkent").trainers(Set.of()).build();
    }

    @Test
    void register_returns201WithGeneratedCredentials() throws Exception {
        Trainee created = trainee(true);
        when(traineeService.createTrainee("John", "Doe", LocalDate.of(2000, 1, 15), "Tashkent"))
                .thenReturn(new Registration<>(created, PASSWORD));
        when(authenticationService.issueToken(USERNAME)).thenReturn("jwt-token");

        String body = """
                {"firstName":"John","lastName":"Doe","dateOfBirth":"2000-01-15","address":"Tashkent"}
                """;

        mockMvc.perform(post("/api/trainees").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.password").value(PASSWORD))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void getProfile_returns200WithProfileFields() throws Exception {
        TrainingType yoga = TrainingType.builder().trainingTypeName("Yoga").build();
        User trainerUser = User.builder().firstName("Anna").lastName("Lee").username("Anna.Lee").build();
        Trainer trainer = Trainer.builder().user(trainerUser).specialization(yoga).build();

        Trainee trainee = trainee(true);
        trainee.setTrainers(Set.of(trainer));
        when(traineeService.getByUsername(USERNAME)).thenReturn(trainee);

        mockMvc.perform(get("/api/trainees/" + USERNAME).principal(PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.trainers[0].username").value("Anna.Lee"))
                .andExpect(jsonPath("$.trainers[0].specialization").value("Yoga"));
    }

    @Test
    void getProfile_returns401_whenMissingAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/trainees/" + USERNAME))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfile_returns403_whenReadingSomeoneElsesProfile() throws Exception {
        mockMvc.perform(get("/api/trainees/Anna.Lee").principal(PRINCIPAL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only access your own profile"));

        verifyNoInteractions(traineeService);
    }

    @Test
    void updateProfile_returns200_andPassesIsActiveThrough() throws Exception {
        Trainee updated = trainee(false);
        when(traineeService.updateFullProfile(USERNAME, "John", "Doe",
                LocalDate.of(2000, 1, 15), "Yunusobod", false)).thenReturn(updated);

        String body = """
                {"username":"John.Doe","firstName":"John","lastName":"Doe",
                 "dateOfBirth":"2000-01-15","address":"Yunusobod","isActive":false}
                """;

        mockMvc.perform(put("/api/trainees/" + USERNAME).principal(PRINCIPAL)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void updateProfile_returns400_whenIsActiveMissing() throws Exception {
        String body = """
                {"username":"John.Doe","firstName":"John","lastName":"Doe"}
                """;

        mockMvc.perform(put("/api/trainees/" + USERNAME).principal(PRINCIPAL)
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_returns403_whenBodyUsernameDoesNotMatchPath() throws Exception {
        String body = """
                {"username":"Someone.Else","firstName":"John","lastName":"Doe","isActive":true}
                """;

        mockMvc.perform(put("/api/trainees/" + USERNAME).principal(PRINCIPAL)
                        .contentType("application/json").content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_returns200() throws Exception {
        mockMvc.perform(delete("/api/trainees/" + USERNAME).principal(PRINCIPAL))
                .andExpect(status().isOk());
    }

    @Test
    void getUnassignedActiveTrainers_returns200WithList() throws Exception {
        TrainingType fitness = TrainingType.builder().trainingTypeName("Fitness").build();
        User trainerUser = User.builder().firstName("Mark").lastName("King").username("Mark.King").build();
        Trainer trainer = Trainer.builder().user(trainerUser).specialization(fitness).build();
        when(traineeService.getUnassignedActiveTrainers(USERNAME)).thenReturn(List.of(trainer));

        mockMvc.perform(get("/api/trainees/" + USERNAME + "/unassigned-trainers")
                        .principal(PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("Mark.King"));
    }

    @Test
    void updateTrainers_returns200WithUpdatedList() throws Exception {
        TrainingType yoga = TrainingType.builder().trainingTypeName("Yoga").build();
        User trainerUser = User.builder().firstName("Anna").lastName("Lee").username("Anna.Lee").build();
        Trainer trainer = Trainer.builder().user(trainerUser).specialization(yoga).build();

        Trainee updated = trainee(true);
        updated.setTrainers(Set.of(trainer));
        when(traineeService.updateTrainers(USERNAME, List.of("Anna.Lee"))).thenReturn(updated);

        String body = """
                {"traineeUsername":"John.Doe","trainers":["Anna.Lee"]}
                """;

        mockMvc.perform(put("/api/trainees/" + USERNAME + "/trainers").principal(PRINCIPAL)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainers[0].username").value("Anna.Lee"));
    }

    @Test
    void getTrainings_returns200WithList_andForwardsOptionalFilters() throws Exception {
        Training training = Training.builder().trainingName("Morning Yoga")
                .trainingDate(LocalDate.of(2026, 9, 1)).trainingDuration(60)
                .trainingType(TrainingType.builder().trainingTypeName("Yoga").build())
                .trainer(Trainer.builder().user(User.builder().firstName("Anna").lastName("Lee").build()).build())
                .build();
        when(trainingService.getTraineeTrainings(USERNAME, null, null, "anna", null))
                .thenReturn(List.of(training));

        mockMvc.perform(get("/api/trainees/" + USERNAME + "/trainings")
                        .principal(PRINCIPAL).param("trainerName", "anna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingName").value("Morning Yoga"))
                .andExpect(jsonPath("$[0].trainerName").value("Anna Lee"));
    }

    @Test
    void setActive_returns200() throws Exception {
        String body = """
                {"username":"John.Doe","isActive":false}
                """;

        mockMvc.perform(patch("/api/trainees/" + USERNAME + "/status").principal(PRINCIPAL)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk());
    }

    @Test
    void setActive_returns409_whenAlreadyInThatState() throws Exception {
        org.mockito.Mockito.doThrow(new com.epam.training.gym.exception.ConflictException("User is already inactive"))
                .when(traineeService).setActive(USERNAME, false);

        String body = """
                {"username":"John.Doe","isActive":false}
                """;

        mockMvc.perform(patch("/api/trainees/" + USERNAME + "/status").principal(PRINCIPAL)
                        .contentType("application/json").content(body))
                .andExpect(status().isConflict());
    }
}