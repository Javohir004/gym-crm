package com.epam.training.gym.service;

import com.epam.training.gym.dao.TraineeDao;
import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.dao.TrainingDao;
import com.epam.training.gym.dao.TrainingTypeDao;
import com.epam.training.gym.dto.TrainingRequest;
import com.epam.training.gym.exception.NotFoundException;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import com.epam.training.gym.model.TrainingType;
import com.epam.training.gym.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


@Service
@Transactional
public class TrainingService {

    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    private TrainingDao trainingDao;
    private TraineeDao traineeDao;
    private TrainerDao trainerDao;
    private TrainingTypeDao trainingTypeDao;
    private AuthenticationService authenticationService;

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

    @Autowired
    public void setTrainingTypeDao(TrainingTypeDao trainingTypeDao) {
        this.trainingTypeDao = trainingTypeDao;
    }

    @Autowired
    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public Training addTraining(String username, String password, TrainingRequest request) {
        authenticationService.authenticate(username, password);

        Validation.requireNotNull(request, "Training data");
        Validation.requireText(request.traineeUsername(), "Trainee username");
        Validation.requireText(request.trainerUsername(), "Trainer username");
        Validation.requireText(request.trainingName(), "Training name");
        Validation.requireText(request.trainingTypeName(), "Training type");
        Validation.requireNotNull(request.trainingDate(), "Training date");
        Validation.requirePositive(request.durationMinutes(), "Training duration");

        Trainee trainee = traineeDao.findByUsername(request.traineeUsername())
                .orElseThrow(() -> new NotFoundException("Trainee not found: " + request.traineeUsername()));
        Trainer trainer = trainerDao.findByUsername(request.trainerUsername())
                .orElseThrow(() -> new NotFoundException("Trainer not found: " + request.trainerUsername()));
        TrainingType type = trainingTypeDao.findByName(request.trainingTypeName().trim())
                .orElseThrow(() -> new NotFoundException("Training type not found: " + request.trainingTypeName()));

        Training training = Training.builder()
                .trainee(trainee)
                .trainer(trainer)
                .trainingName(request.trainingName().trim())
                .trainingType(type)
                .trainingDate(request.trainingDate())
                .trainingDuration(request.durationMinutes())
                .build();

        Training saved = trainingDao.save(training);
        log.info("Training added: name='{}' trainee={} trainer={} type={} date={} duration={}min",
                saved.getTrainingName(), request.traineeUsername(), request.trainerUsername(),
                type.getTrainingTypeName(), saved.getTrainingDate(), saved.getTrainingDuration());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Training> getTraineeTrainings(String username, String password, LocalDate fromDate,
                                              LocalDate toDate, String trainerName, String trainingTypeName) {
        authenticationService.authenticateTrainee(username, password);
        log.debug("Searching trainings of trainee username={} from={} to={} trainer='{}' type='{}'",
                username, fromDate, toDate, trainerName, trainingTypeName);
        return trainingDao.findByTraineeUsername(username, fromDate, toDate, trainerName, trainingTypeName);
    }

    @Transactional(readOnly = true)
    public List<Training> getTrainerTrainings(String username, String password, LocalDate fromDate,
                                              LocalDate toDate, String traineeName) {
        authenticationService.authenticateTrainer(username, password);
        log.debug("Searching trainings of trainer username={} from={} to={} trainee='{}'",
                username, fromDate, toDate, traineeName);
        return trainingDao.findByTrainerUsername(username, fromDate, toDate, traineeName);
    }
}