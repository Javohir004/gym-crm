package integration;

import com.epam.training.gym.GymCrmApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GymCrmApplication.class,
        properties = "spring.datasource.url=jdbc:h2:mem:actuator-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("local")
class ActuatorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void health_containsAllCustomIndicators() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.trainingTypes.status").value("UP"))
                .andExpect(jsonPath("$.components.trainerAvailability.status").value("UP"))
                .andExpect(jsonPath("$.components.databaseResponseTime.status").value("UP"));
    }

    @Test
    void prometheus_exposesCustomMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("gym_trainers_active")))
                .andExpect(content().string(containsString("gym_registrations_total")));
    }
}