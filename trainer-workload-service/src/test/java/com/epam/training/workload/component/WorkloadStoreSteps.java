package com.epam.training.workload.component;

import com.epam.training.workload.dto.WorkloadRequest;
import com.epam.training.workload.model.ActionType;
import com.epam.training.workload.model.TrainerSummary;
import com.epam.training.workload.repository.TrainerSummaryRepository;
import com.epam.training.workload.service.TrainerWorkloadService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class WorkloadStoreSteps {

    private final TrainerSummaryRepository repository;
    private final TrainerWorkloadService service;

    public WorkloadStoreSteps(TrainerSummaryRepository repository, TrainerWorkloadService service) {
        this.repository = repository;
        this.service = service;
    }

    @Given("the workload store is empty")
    public void theWorkloadStoreIsEmpty() {
        repository.deleteAll();
    }

    @Given("trainer {string} {string} {string} has a training on {string} for {int} minutes")
    public void trainerHasATraining(String username, String firstName, String lastName, String date, int minutes) {
        service.process(new WorkloadRequest(username, firstName, lastName, true, LocalDate.parse(date), minutes,
                ActionType.ADD));
    }

    @Then("the trainer {string} has {int} minutes in {int} month {int}")
    public void theTrainerHasMinutes(String username, int minutes, int year, int month) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        long actual = -1;
        while (Instant.now().isBefore(deadline)) {
            actual = repository.findByTrainerUsername(username).map(s -> s.minutesIn(year, month)).orElse(-1L);
            if (actual == minutes) {
                return;
            }
            Thread.sleep(50);
        }
        fail("expected " + minutes + " minutes but was " + actual);
    }

    @Then("the summary of {string} is stored in MongoDB as {string} {string} with status active")
    public void theSummaryIsStored(String username, String firstName, String lastName) {
        TrainerSummary summary = repository.findByTrainerUsername(username).orElseThrow();
        assertEquals(firstName, summary.getTrainerFirstName());
        assertEquals(lastName, summary.getTrainerLastName());
        assertTrue(summary.getTrainerStatus());
    }

    @Then("no summary is stored for trainer {string}")
    public void noSummaryIsStored(String username) {
        Optional<TrainerSummary> summary = repository.findByTrainerUsername(username);
        assertTrue(summary.isEmpty());
    }
}
