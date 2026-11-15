package com.epam.training.workload.repository;

import com.epam.training.workload.model.TrainerWorkload;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

@Repository
public class InMemoryTrainerWorkloadRepository implements TrainerWorkloadRepository {

    private final Map<String, TrainerWorkload> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<TrainerWorkload> findByUsername(String username) {
        return Optional.ofNullable(storage.get(username));
    }

    @Override
    public TrainerWorkload update(String username, UnaryOperator<TrainerWorkload> updater) {
        return storage.compute(username, (key, current) -> updater.apply(current));
    }
}
