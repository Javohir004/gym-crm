package com.epam.training.workload.service;

import com.epam.training.workload.dto.MonthlyWorkloadResponse;
import com.epam.training.workload.dto.TrainerSummaryResponse;
import com.epam.training.workload.dto.WorkloadRequest;
import com.epam.training.workload.exception.NotFoundException;
import com.epam.training.workload.model.TrainerSummary;
import com.epam.training.workload.repository.TrainerSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Month;
import java.util.List;

@Service
public class TrainerWorkloadService {

    private static final Logger log = LoggerFactory.getLogger(TrainerWorkloadService.class);

    private final TrainerSummaryRepository repository;

    public TrainerWorkloadService(TrainerSummaryRepository repository) {
        this.repository = repository;
    }

    public void process(WorkloadRequest request) {
        switch (request.actionType()) {
            case ADD -> add(request);
            case DELETE -> delete(request);
        }
    }

    public TrainerSummaryResponse getSummary(String username) {
        log.debug("Reading training summary of trainer={}", username);
        return TrainerSummaryResponse.from(findTrainer(username));
    }

    public MonthlyWorkloadResponse getMonthlyWorkload(String username, int year, Month month) {
        long minutes = findTrainer(username).minutesIn(year, month.getValue());
        log.debug("Training summary of trainer={} in {}-{}: {} min", username, year, month, minutes);
        return new MonthlyWorkloadResponse(username, year, month, minutes);
    }

    public List<TrainerSummaryResponse> findByName(String firstName, String lastName) {
        List<TrainerSummary> found = repository.findByTrainerFirstNameAndTrainerLastName(firstName, lastName);
        log.debug("Search by name firstName={} lastName={}: {} trainer(s)", firstName, lastName, found.size());
        return found.stream().map(TrainerSummaryResponse::from).toList();
    }

    private void add(WorkloadRequest request) {
        TrainerSummary summary = repository.findByTrainerUsername(request.trainerUsername())
                .map(existing -> {
                    log.debug("Found training summary of trainer={}", request.trainerUsername());
                    return existing;
                })
                .orElseGet(() -> {
                    log.info("No training summary for trainer={} yet, creating one", request.trainerUsername());
                    return new TrainerSummary(request.trainerUsername(), request.trainerFirstName(),
                            request.trainerLastName(), request.isActive());
                });
        summary.updateProfile(request.trainerFirstName(), request.trainerLastName(), request.isActive());
        long total = summary.addMinutes(request.trainingDate(), request.trainingDuration());
        repository.save(summary);
        log.info("Added {} min for trainer={} on {}; {}-{} total is now {} min", request.trainingDuration(),
                request.trainerUsername(), request.trainingDate(), request.trainingDate().getYear(),
                request.trainingDate().getMonth(), total);
    }

    private void delete(WorkloadRequest request) {
        TrainerSummary summary = repository.findByTrainerUsername(request.trainerUsername()).orElse(null);
        if (summary == null) {
            log.warn("Cancelled training of unknown trainer={} on {}: nothing to subtract",
                    request.trainerUsername(), request.trainingDate());
            return;
        }
        summary.updateProfile(request.trainerFirstName(), request.trainerLastName(), request.isActive());
        long removed = summary.removeMinutes(request.trainingDate(), request.trainingDuration());
        repository.save(summary);
        if (removed < request.trainingDuration()) {
            log.warn("Cancelled training of trainer={} on {}: only {} of {} min were recorded",
                    request.trainerUsername(), request.trainingDate(), removed, request.trainingDuration());
        } else {
            log.info("Removed {} min for trainer={} on {}", removed, request.trainerUsername(),
                    request.trainingDate());
        }
    }

    private TrainerSummary findTrainer(String username) {
        return repository.findByTrainerUsername(username)
                .orElseThrow(() -> new NotFoundException("No workload recorded for trainer: " + username));
    }
}
