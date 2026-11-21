package com.epam.training.workload.repository;

import com.epam.training.workload.model.TrainerSummary;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TrainerSummaryRepository extends MongoRepository<TrainerSummary, String> {

    Optional<TrainerSummary> findByTrainerUsername(String trainerUsername);

    List<TrainerSummary> findByTrainerFirstNameAndTrainerLastName(String trainerFirstName, String trainerLastName);
}
