package integration;

import com.epam.training.gym.GymCrmApplication;
import com.jayway.jsonpath.JsonPath;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(classes = GymCrmApplication.class,
        properties = "spring.datasource.url=jdbc:h2:mem:metrics-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("local")
class MetricsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MeterRegistry registry;

    private double count(String name, String... tags) {
        return registry.get(name).tags(tags).counter().count();
    }

    private String register(String path, String json) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void registeringATrainee_returns201_andCountsIt() throws Exception {
        double before = count("gym.registrations", "role", "trainee");

        register("/api/trainees",
                "{\"firstName\":\"Metric\",\"lastName\":\"Trainee\",\"dateOfBirth\":\"2000-01-15\",\"address\":\"Tashkent\"}");

        assertEquals(before + 1, count("gym.registrations", "role", "trainee"));
    }

    @Test
    void registeringATrainer_returns201_andCountsIt() throws Exception {
        double before = count("gym.registrations", "role", "trainer");

        register("/api/trainers", "{\"firstName\":\"Metric\",\"lastName\":\"Trainer\",\"specialization\":\"Yoga\"}");

        assertEquals(before + 1, count("gym.registrations", "role", "trainer"));
    }

    @Test
    void wrongPasswordLogin_returns401_andCountsTheFailure() throws Exception {
        double before = count("gym.authentication.failures");

        mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"No.Body\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());

        assertEquals(before + 1, count("gym.authentication.failures"));
    }

    @Test
    void addingATraining_returns200_andCountsIt() throws Exception {
        String trainee = register("/api/trainees", "{\"firstName\":\"Train\",\"lastName\":\"Ee\"}");
        String trainer = register("/api/trainers", "{\"firstName\":\"Train\",\"lastName\":\"Er\",\"specialization\":\"Yoga\"}");
        String traineeUser = JsonPath.read(trainee, "$.username");
        String trainerUser = JsonPath.read(trainer, "$.username");
        String bearer = "Bearer " + JsonPath.read(trainee, "$.token");

        double before = count("gym.trainings.added");

        mockMvc.perform(post("/api/trainings")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"traineeUsername\":\"" + traineeUser + "\",\"trainerUsername\":\"" + trainerUser
                                + "\",\"trainingName\":\"Metric session\",\"trainingDate\":\"2026-10-20\",\"trainingDuration\":60}"))
                .andExpect(status().isOk());

        assertEquals(before + 1, count("gym.trainings.added"));
    }
}