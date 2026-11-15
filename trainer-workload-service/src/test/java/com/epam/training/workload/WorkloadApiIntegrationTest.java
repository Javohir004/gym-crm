package com.epam.training.workload;

import com.epam.training.workload.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkloadApiIntegrationTest {

    private static final String BASE = "/api/v1/workloads";

    @Autowired
    private MockMvc mockMvc;

    private static String body(String username, String date, int minutes, String action) {
        return """
                {"trainerUsername":"%s","trainerFirstName":"Anna","trainerLastName":"Lee","isActive":true,
                 "trainingDate":"%s","trainingDuration":%d,"actionType":"%s"}
                """.formatted(username, date, minutes, action);
    }

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestTokens.valid());
    }

    private void send(String username, String date, int minutes, String action) throws Exception {
        mockMvc.perform(authorized(post(BASE)).contentType(MediaType.APPLICATION_JSON)
                        .content(body(username, date, minutes, action)))
                .andExpect(status().isOk());
    }

    @Test
    void addThenDelete_updatesMonthlyAndSummaryViews() throws Exception {
        send("Api.Trainer", "2026-03-01", 60, "ADD");
        send("Api.Trainer", "2026-03-15", 45, "ADD");
        send("Api.Trainer", "2026-03-15", 45, "DELETE");

        mockMvc.perform(authorized(get(BASE + "/Api.Trainer/years/2026/months/3")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("MARCH"))
                .andExpect(jsonPath("$.trainingSummaryDuration").value(60));

        mockMvc.perform(authorized(get(BASE + "/Api.Trainer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value("Api.Trainer"))
                .andExpect(jsonPath("$.trainerStatus").value(true))
                .andExpect(jsonPath("$.years[0].year").value(2026))
                .andExpect(jsonPath("$.years[0].months[0].trainingSummaryDuration").value(60));
    }

    @Test
    void withoutToken_returns401_inJson() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Api.Trainer", "2026-03-01", 60, "ADD")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void withExpiredToken_returns401() throws Exception {
        String expired = TestTokens.token(TestTokens.SECRET, "gym-crm", "workload",
                Instant.now().minus(1, ChronoUnit.MINUTES));

        mockMvc.perform(get(BASE + "/Api.Trainer").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidRequest_returns400_namingTheFields() throws Exception {
        mockMvc.perform(authorized(post(BASE)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trainerUsername\":\"Api.Trainer\",\"trainingDuration\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", matchesPattern(".*actionType.*trainingDuration.*")));
    }

    @Test
    void unknownActionType_returns400() throws Exception {
        mockMvc.perform(authorized(post(BASE)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Api.Trainer", "2026-03-01", 60, "UPDATE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void unknownTrainer_returns404() throws Exception {
        mockMvc.perform(authorized(get(BASE + "/No.Such.Trainer")))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidMonth_returns400() throws Exception {
        send("Month.Trainer", "2026-03-01", 60, "ADD");

        mockMvc.perform(authorized(get(BASE + "/Month.Trainer/years/2026/months/13")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void incomingTransactionId_isKept_soOneIdSpansBothServices() throws Exception {
        mockMvc.perform(authorized(get(BASE + "/No.Such.Trainer")).header("X-Transaction-Id", "abc-123"))
                .andExpect(header().string("X-Transaction-Id", "abc-123"))
                .andExpect(jsonPath("$.transactionId").value("abc-123"));
    }

    @Test
    void unsafeTransactionId_isReplacedWithANewOne() throws Exception {
        mockMvc.perform(authorized(get(BASE + "/No.Such.Trainer")).header("X-Transaction-Id", "bad\nid"))
                .andExpect(header().string("X-Transaction-Id", matchesPattern("[0-9a-f-]{36}")));
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
