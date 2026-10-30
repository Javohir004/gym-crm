package integration;

import com.epam.training.gym.GymCrmApplication;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import support.BasicAuthHeader;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full register -> login -> change password flow against a real database. */
@SpringBootTest(classes = GymCrmApplication.class,
        properties = "spring.datasource.url=jdbc:h2:mem:auth-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registeredPassword_isStoredAsBcryptHash_andStillLogsIn() throws Exception {
        String response = mockMvc.perform(post("/api/trainees").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Hash\",\"lastName\":\"Check\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String username = JsonPath.read(response, "$.username");
        String password = JsonPath.read(response, "$.password");

        String stored = jdbcTemplate.queryForObject(
                "select password from users where username = ?", String.class, username);
        assertNotEquals(password, stored);
        assertTrue(stored.startsWith("$2a$"));

        mockMvc.perform(get("/api/login").header("Authorization", BasicAuthHeader.value(username, password)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/login").header("Authorization", BasicAuthHeader.value(username, stored)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changedPassword_replacesTheOldOne() throws Exception {
        String response = mockMvc.perform(post("/api/trainees").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Change\",\"lastName\":\"Me\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String username = JsonPath.read(response, "$.username");
        String password = JsonPath.read(response, "$.password");

        mockMvc.perform(put("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"oldPassword\":\"" + password
                                + "\",\"newPassword\":\"BrandNew123\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/login").header("Authorization", BasicAuthHeader.value(username, "BrandNew123")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/login").header("Authorization", BasicAuthHeader.value(username, password)))
                .andExpect(status().isUnauthorized());
    }
}
