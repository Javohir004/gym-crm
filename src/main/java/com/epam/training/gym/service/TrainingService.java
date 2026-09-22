package com.epam.training.gym.service;

import com.epam.training.gym.dao.TraineeDao;
import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.dao.TrainingDao;
import com.epam.training.gym.model.Training;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Service
public class TrainingService {

    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    private TrainingDao trainingDao;
    private TraineeDao traineeDao;
    private TrainerDao trainerDao;

    @Autowired
    public void setTrainingDao(TrainingDao trainingDao) {
        this.trainingDao = trainingDao;
    }

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    public Training create(Training training) {
        if (training == null) {
            throw new IllegalArgumentException("Training must not be null");
        }
        if (training.getTraineeId() == null || traineeDao.select(training.getTraineeId()) == null) {
            log.warn("Cannot create training: unknown trainee id={}", training.getTraineeId());
            throw new IllegalArgumentException("Trainee not found: " + training.getTraineeId());
        }
        if (training.getTrainerId() == null || trainerDao.select(training.getTrainerId()) == null) {
            log.warn("Cannot create training: unknown trainer id={}", training.getTrainerId());
            throw new IllegalArgumentException("Trainer not found: " + training.getTrainerId());
        }

        training.setTrainingId(nextId());
        Training created = trainingDao.create(training);
        log.info("Training created: id={}, name='{}'", created.getTrainingId(), created.getTrainingName());
        return created;
    }

    public Optional<Training> select(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        Training training = trainingDao.select(id);
        if (training == null) {
            log.debug("No training found with id={}", id);
        }
        return Optional.ofNullable(training);
    }

    public List<Training> selectAll() {
        List<Training> all = trainingDao.selectAll();
        log.debug("Selected {} training(s)", all.size());
        return all;
    }

    private Long nextId() {
        return trainingDao.selectAll().stream()
                .map(Training::getTrainingId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1;
    }
}