package component;

import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class AuthenticationSteps {

    private final MockMvc mockMvc;
    private final GymWorld world;

    public AuthenticationSteps(MockMvc mockMvc, GymWorld world) {
        this.mockMvc = mockMvc;
        this.world = world;
    }

    private void perform(MockHttpServletRequestBuilder request) throws Exception {
        world.response(mockMvc.perform(request).andReturn().getResponse());
    }

    private void login(String username, String password) throws Exception {
        perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
    }

    @When("the trainee logs in with the correct password")
    public void theTraineeLogsInWithTheCorrectPassword() throws Exception {
        login(world.trainee().username(), world.trainee().password());
    }

    @When("the trainee logs in with the password {string}")
    public void theTraineeLogsInWithThePassword(String password) throws Exception {
        login(world.trainee().username(), password);
    }

    @When("the trainee logs in with the password {string} {int} times")
    public void theTraineeLogsInWithThePasswordTimes(String password, int times) throws Exception {
        for (int i = 0; i < times; i++) {
            login(world.trainee().username(), password);
        }
    }

    @When("{string} logs in with the password {string}")
    public void userLogsInWithThePassword(String username, String password) throws Exception {
        login(username, password);
    }

    @When("the trainee requests their profile with their token")
    public void theTraineeRequestsTheirProfileWithTheirToken() throws Exception {
        perform(get("/api/trainees/" + world.trainee().username()).header("Authorization", world.trainee().bearer()));
    }

    @When("the trainee requests their profile without a token")
    public void theTraineeRequestsTheirProfileWithoutAToken() throws Exception {
        perform(get("/api/trainees/" + world.trainee().username()));
    }

    @When("the trainee requests their profile with a tampered token")
    public void theTraineeRequestsTheirProfileWithATamperedToken() throws Exception {
        String token = world.trainee().token();
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");
        perform(get("/api/trainees/" + world.trainee().username()).header("Authorization", "Bearer " + tampered));
    }
}
