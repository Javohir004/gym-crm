package integration;

import com.epam.training.gym.GymCrmApplication;
import com.epam.training.gym.dao.UserDao;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole security chain with real HTTP-style requests: registration, login, JWT,
 * brute-force blocking, logout and CORS.
 */
@SpringBootTest(classes = GymCrmApplication.class,
        properties = "spring.datasource.url=jdbc:h2:mem:security-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserDao userDao;
    @Autowired
    private PlatformTransactionManager transactionManager;

    /** Registered account: generated username, generated password and the token from registration. */
    private record Account(String username, String password, String token) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    private Account registerTrainee() throws Exception {
        String lastName = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String json = mockMvc.perform(post("/api/trainees").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Sec\",\"lastName\":\"" + lastName + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Account(JsonPath.read(json, "$.username"), JsonPath.read(json, "$.password"),
                JsonPath.read(json, "$.token"));
    }

    private String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private String login(String username, String password) throws Exception {
        String json = mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    // ---------- who needs a token ----------

    @Test
    void protectedEndpoints_return401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/trainees/John.Doe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mockMvc.perform(get("/api/training-types")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/trainings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProfile_needsNoToken_andReturnsOne() throws Exception {
        Account trainee = registerTrainee();

        assertFalse(trainee.token().isBlank());

        mockMvc.perform(post("/api/trainers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Sec\",\"lastName\":\"Trainer\",\"specialization\":\"Yoga\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void registrationToken_opensProtectedEndpoints() throws Exception {
        Account trainee = registerTrainee();

        mockMvc.perform(get("/api/trainees/" + trainee.username()).header("Authorization", trainee.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Sec"));
        mockMvc.perform(get("/api/training-types").header("Authorization", trainee.bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void tamperedToken_isRejected() throws Exception {
        Account trainee = registerTrainee();
        String token = trainee.token();
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");

        mockMvc.perform(get("/api/training-types").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void basicAuth_isNoLongerAccepted() throws Exception {
        mockMvc.perform(get("/api/training-types").header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void user_cannotReadSomeoneElsesProfile() throws Exception {
        Account first = registerTrainee();
        Account second = registerTrainee();

        mockMvc.perform(get("/api/trainees/" + second.username()).header("Authorization", first.bearer()))
                .andExpect(status().isForbidden());
    }

    // ---------- password storage ----------

    @Test
    void password_isStoredAsSaltedBcryptHash() throws Exception {
        Account trainee = registerTrainee();

        String stored = new TransactionTemplate(transactionManager).execute(status ->
                userDao.findByUsername(trainee.username()).orElseThrow().getPassword());

        assertNotEquals(trainee.password(), stored);
        assertTrue(stored.startsWith("$2"), "expected a BCrypt hash but was: " + stored.substring(0, 4));
    }

    // ---------- login ----------

    @Test
    void login_returnsWorkingToken_forCorrectPassword() throws Exception {
        Account trainee = registerTrainee();

        String token = login(trainee.username(), trainee.password());

        mockMvc.perform(get("/api/trainees/" + trainee.username()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void login_returns401_forWrongPassword_andForUnknownUser() throws Exception {
        Account trainee = registerTrainee();

        mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(trainee.username(), "wrongPassword1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
        mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("Nobody.Here", "whatever12345")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_thenOldPasswordStopsWorking() throws Exception {
        Account trainee = registerTrainee();

        mockMvc.perform(put("/api/login").header("Authorization", trainee.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + trainee.username() + "\",\"oldPassword\":\""
                                + trainee.password() + "\",\"newPassword\":\"NewSecret12345\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(trainee.username(), trainee.password())))
                .andExpect(status().isUnauthorized());
        login(trainee.username(), "NewSecret12345");
    }

    // ---------- brute force ----------

    @Test
    void threeWrongPasswords_blockTheUser_evenForTheCorrectPassword() throws Exception {
        Account trainee = registerTrainee();

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(trainee.username(), "wrongPassword1")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(trainee.username(), trainee.password())))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("minute")));
    }

    @Test
    void twoWrongPasswords_doNotBlock_andASuccessResetsTheCounter() throws Exception {
        Account trainee = registerTrainee();

        for (int round = 0; round < 2; round++) {
            for (int i = 0; i < 2; i++) {
                mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(trainee.username(), "wrongPassword1")))
                        .andExpect(status().isUnauthorized());
            }
            login(trainee.username(), trainee.password());
        }
    }

    // ---------- logout ----------

    @Test
    void logout_makesTheTokenUnusable() throws Exception {
        Account trainee = registerTrainee();
        String token = login(trainee.username(), trainee.password());
        String bearer = "Bearer " + token;

        mockMvc.perform(get("/api/training-types").header("Authorization", bearer)).andExpect(status().isOk());
        mockMvc.perform(post("/api/logout").header("Authorization", bearer)).andExpect(status().isOk());

        mockMvc.perform(get("/api/training-types").header("Authorization", bearer))
                .andExpect(status().isUnauthorized());
        // other tokens of the same user are not affected
        mockMvc.perform(get("/api/training-types").header("Authorization", trainee.bearer()))
                .andExpect(status().isOk());
    }

    // ---------- CORS ----------

    @Test
    void cors_preflightFromAllowedOrigin_isAccepted() throws Exception {
        mockMvc.perform(options("/api/trainees")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void cors_preflightFromUnknownOrigin_isRejected() throws Exception {
        mockMvc.perform(options("/api/trainees")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}