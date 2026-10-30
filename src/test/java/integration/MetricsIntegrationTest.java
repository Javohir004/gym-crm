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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sends real HTTP requests through the Spring proxies, so it proves that
 * the metrics aspect is wired correctly into the services.
 */
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

        register("/api/v1/trainees",
                "{\"firstName\":\"Metric\",\"lastName\":\"Trainee\",\"dateOfBirth\":\"2000-01-15\",\"address\":\"Tashkent\"}");

        assertEquals(before + 1, count("gym.registrations", "role", "trainee"));
    }

    @Test
    void registeringATrainer_returns201_andCountsIt() throws Exception {
        double before = count("gym.registrations", "role", "trainer");

        register("/api/v1/trainers", "{\"firstName\":\"Metric\",\"lastName\":\"Trainer\",\"specialization\":\"Yoga\"}");

        assertEquals(before + 1, count("gym.registrations", "role", "trainer"));
    }

    @Test
    void wrongPasswordLogin_returns401_andCountsTheFailure() throws Exception {
        double before = count("gym.authentication.failures");

        String basic = "Basic " + Base64.getEncoder().encodeToString("No.Body:wrong".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/v1/login").header("Authorization", basic))
                .andExpect(status().isUnauthorized());

        assertEquals(before + 1, count("gym.authentication.failures"));
    }

    @Test
    void addingATraining_returns200_andCountsIt() throws Exception {
        String trainee = register("/api/v1/trainees", "{\"firstName\":\"Train\",\"lastName\":\"Ee\"}");
        String trainer = register("/api/v1/trainers", "{\"firstName\":\"Train\",\"lastName\":\"Er\",\"specialization\":\"Yoga\"}");
        String traineeUser = JsonPath.read(trainee, "$.username");
        String traineePass = JsonPath.read(trainee, "$.password");
        String trainerUser = JsonPath.read(trainer, "$.username");
        String basic = "Basic " + Base64.getEncoder()
                .encodeToString((traineeUser + ":" + traineePass).getBytes(StandardCharsets.UTF_8));

        double before = count("gym.trainings.added");

        mockMvc.perform(post("/api/v1/trainings")
                        .header("Authorization", basic)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"traineeUsername\":\"" + traineeUser + "\",\"trainerUsername\":\"" + trainerUser
                                + "\",\"trainingName\":\"Metric session\",\"trainingDate\":\"2026-10-20\",\"trainingDuration\":60}"))
                .andExpect(status().isOk());

        assertEquals(before + 1, count("gym.trainings.added"));
    }
}