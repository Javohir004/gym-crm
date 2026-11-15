package com.epam.training.workload.repository;

import com.epam.training.workload.model.TrainerWorkload;

import java.util.Optional;
import java.util.function.UnaryOperator;

public interface TrainerWorkloadRepository {

    Optional<TrainerWorkload> findByUsername(String username);

    TrainerWorkload update(String username, UnaryOperator<TrainerWorkload> updater);
}
