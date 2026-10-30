package exception;

import com.epam.training.gym.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Spring MVC's own exceptions must map to their 4xx status in our ErrorResponse shape, never to 500. */
class SpringMvcErrorStatusTest {

    record Body(String name) {
    }

    @RestController
    static class SampleController {

        @GetMapping("/sample")
        String get(@RequestParam String username, @RequestParam(required = false) LocalDate from) {
            return "ok";
        }

        @PostMapping(value = "/sample", consumes = MediaType.APPLICATION_JSON_VALUE)
        String post(@RequestBody Body body) {
            return "ok";
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SampleController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void missingRequestParameter_returns400() throws Exception {
        mockMvc.perform(get("/sample"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Missing required parameter: username"));
    }

    @Test
    void wrongParameterType_returns400() throws Exception {
        mockMvc.perform(get("/sample").param("username", "a").param("from", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter: from"));
    }

    @Test
    void malformedJson_returns400_withoutParserDetails() throws Exception {
        mockMvc.perform(post("/sample").contentType(MediaType.APPLICATION_JSON).content("{ broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"))
                .andExpect(jsonPath("$.message", not(containsString("Jackson"))));
    }

    @Test
    void unsupportedMethod_returns405_withAllowHeader() throws Exception {
        mockMvc.perform(delete("/sample"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(header().exists("Allow"));
    }

    @Test
    void unsupportedMediaType_returns415() throws Exception {
        mockMvc.perform(post("/sample").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }
}
