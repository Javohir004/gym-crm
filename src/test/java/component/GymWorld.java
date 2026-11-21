package component;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.spring.ScenarioScope;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Component
@ScenarioScope
public class GymWorld {

    public record Account(String username, String password, String token) {
        public String bearer() {
            return "Bearer " + token;
        }
    }

    private final MockMvc mockMvc;
    private Account trainee;
    private Account trainer;
    private MockHttpServletResponse response;

    public GymWorld(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public Account register(String path, String json) throws Exception {
        String body = mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn().getResponse().getContentAsString();
        return new Account(JsonPath.read(body, "$.username"), JsonPath.read(body, "$.password"),
                JsonPath.read(body, "$.token"));
    }

    public Account trainee() {
        return trainee;
    }

    public void trainee(Account trainee) {
        this.trainee = trainee;
    }

    public Account trainer() {
        return trainer;
    }

    public void trainer(Account trainer) {
        this.trainer = trainer;
    }

    public MockHttpServletResponse response() {
        return response;
    }

    public void response(MockHttpServletResponse response) {
        this.response = response;
    }
}
