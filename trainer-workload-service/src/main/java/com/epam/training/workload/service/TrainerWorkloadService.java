package com.epam.training.workload.service;

import com.epam.training.workload.dto.MonthlyWorkloadResponse;
import com.epam.training.workload.dto.TrainerSummaryResponse;
import com.epam.training.workload.dto.WorkloadRequest;
import com.epam.training.workload.exception.NotFoundException;
import com.epam.training.workload.model.TrainerWorkload;
import com.epam.training.workload.repository.TrainerWorkloadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Month;

@Service
public class TrainerWorkloadService {

    private static final Logger log = LoggerFactory.getLogger(TrainerWorkloadService.class);

    private final TrainerWorkloadRepository repository;

    public TrainerWorkloadService(TrainerWorkloadRepository repository) {
        this.repository = repository;
    }

    public void process(WorkloadRequest request) {
        switch (request.actionType()) {
            case ADD -> add(request);
            case DELETE -> delete(request);
        }
    }

    public TrainerSummaryResponse getSummary(String username) {
        log.debug("Reading workload summary for trainer={}", username);
        return TrainerSummaryResponse.from(findTrainer(username));
    }

    public MonthlyWorkloadResponse getMonthlyWorkload(String username, int year, Month month) {
        int minutes = findTrainer(username).minutesIn(year, month);
        log.debug("Workload of trainer={} in {}-{}: {} min", username, year, month, minutes);
        return new MonthlyWorkloadResponse(username, year, month, minutes);
    }

    private void add(WorkloadRequest request) {
        TrainerWorkload updated = repository.update(request.trainerUsername(), current -> {
            TrainerWorkload workload = current != null ? current
                    : new TrainerWorkload(request.trainerUsername(), request.trainerFirstName(),
                    request.trainerLastName(), request.isActive());
            workload.updateProfile(request.trainerFirstName(), request.trainerLastName(), request.isActive());
            workload.addMinutes(request.trainingDate(), request.trainingDuration());
            return workload;
        });
        log.info("Added {} min for trainer={} on {}; month total is now {} min",
                request.trainingDuration(), request.trainerUsername(), request.trainingDate(),
                updated.minutesIn(request.trainingDate().getYear(), request.trainingDate().getMonth()));
    }

    private void delete(WorkloadRequest request) {
        int[] removed = {0};
        repository.update(request.trainerUsername(), current -> {
            if (current == null) {
                return null;
            }
            current.updateProfile(request.trainerFirstName(), request.trainerLastName(), request.isActive());
            removed[0] = current.removeMinutes(request.trainingDate(), request.trainingDuration());
            return current;
        });
        if (removed[0] < request.trainingDuration()) {
            log.warn("Cancelled training of trainer={} on {}: only {} of {} min were recorded",
                    request.trainerUsername(), request.trainingDate(), removed[0], request.trainingDuration());
        } else {
            log.info("Removed {} min for trainer={} on {}", removed[0], request.trainerUsername(),
                    request.trainingDate());
        }
    }

    private TrainerWorkload findTrainer(String username) {
        return repository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("No workload recorded for trainer: " + username));
    }
}
