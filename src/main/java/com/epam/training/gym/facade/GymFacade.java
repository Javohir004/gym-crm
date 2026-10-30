package com.epam.training.gym.facade;


import com.epam.training.gym.dto.requests.TrainingRequest;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import com.epam.training.gym.model.TrainingType;
import com.epam.training.gym.model.User;
import com.epam.training.gym.service.AuthenticationService;
import com.epam.training.gym.service.TraineeService;
import com.epam.training.gym.service.TrainerService;
import com.epam.training.gym.service.TrainingService;
import com.epam.training.gym.service.TrainingTypeService;
import com.epam.training.gym.service.UserAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;


@Component
public class GymFacade {

    private static final Logger log = LoggerFactory.getLogger(GymFacade.class);

    private final TraineeService traineeService;
    private final TrainerService trainerService;
    private final TrainingService trainingService;
    private final AuthenticationService authenticationService;
    private final UserAccountService userAccountService;
    private final TrainingTypeService trainingTypeService;

    public GymFacade(TraineeService traineeService,
                     TrainerService trainerService,
                     TrainingService trainingService,
                     AuthenticationService authenticationService,
                     UserAccountService userAccountService,
                     TrainingTypeService trainingTypeService) {
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
        this.authenticationService = authenticationService;
        this.userAccountService = userAccountService;
        this.trainingTypeService = trainingTypeService;
        log.debug("GymFacade initialized");
    }

    // ---------- Trainer ----------


    public Trainer createTrainer(String firstName, String lastName, String specialization) {
        log.info("Facade: createTrainer");
        return trainerService.createTrainer(firstName, lastName, specialization);
    }

    public boolean trainerCredentialsMatch(String username, String password) {
        log.info("Facade: trainerCredentialsMatch username={}", username);
        return trainerService.credentialsMatch(username, password);
    }

    public Trainer getTrainer(String username, String password) {
        log.info("Facade: getTrainer username={}", username);
        return trainerService.getByUsername(username, password);
    }

    public void changeTrainerPassword(String username, String password, String newPassword) {
        log.info("Facade: changeTrainerPassword username={}", username);
        trainerService.changePassword(username, password, newPassword);
    }

    public Trainer updateTrainer(String username, String password,
                                 String firstName, String lastName, String specialization) {
        log.info("Facade: updateTrainer username={}", username);
        return trainerService.update(username, password, firstName, lastName, specialization);
    }

    public Trainer updateTrainerProfile(String username, String password,
                                        String firstName, String lastName, boolean isActive) {
        log.info("Facade: updateTrainerProfile username={}", username);
        return trainerService.updateFullProfile(username, password, firstName, lastName, isActive);
    }

    public boolean toggleTrainerActive(String username, String password) {
        log.info("Facade: toggleTrainerActive username={}", username);
        return trainerService.toggleActive(username, password);
    }

    public void setTrainerActive(String username, String password, boolean active) {
        log.info("Facade: setTrainerActive username={} active={}", username, active);
        trainerService.setActive(username, password, active);
    }

    // ---------- Trainee ----------


    public Trainee createTrainee(String firstName, String lastName, LocalDate dateOfBirth, String address) {
        log.info("Facade: createTrainee");
        return traineeService.createTrainee(firstName, lastName, dateOfBirth, address);
    }

    public boolean traineeCredentialsMatch(String username, String password) {
        log.info("Facade: traineeCredentialsMatch username={}", username);
        return traineeService.credentialsMatch(username, password);
    }

    public Trainee getTrainee(String username, String password) {
        log.info("Facade: getTrainee username={}", username);
        return traineeService.getByUsername(username, password);
    }

    public void changeTraineePassword(String username, String password, String newPassword) {
        log.info("Facade: changeTraineePassword username={}", username);
        traineeService.changePassword(username, password, newPassword);
    }

    public Trainee updateTrainee(String username, String password,
                                 String firstName, String lastName, LocalDate dateOfBirth, String address) {
        log.info("Facade: updateTrainee username={}", username);
        return traineeService.update(username, password, firstName, lastName, dateOfBirth, address);
    }

    public Trainee updateTraineeProfile(String username, String password, String firstName, String lastName,
                                        LocalDate dateOfBirth, String address, boolean isActive) {
        log.info("Facade: updateTraineeProfile username={}", username);
        return traineeService.updateFullProfile(username, password, firstName, lastName, dateOfBirth, address, isActive);
    }

    public boolean toggleTraineeActive(String username, String password) {
        log.info("Facade: toggleTraineeActive username={}", username);
        return traineeService.toggleActive(username, password);
    }

    public void setTraineeActive(String username, String password, boolean active) {
        log.info("Facade: setTraineeActive username={} active={}", username, active);
        traineeService.setActive(username, password, active);
    }

    public void deleteTrainee(String username, String password) {
        log.info("Facade: deleteTrainee username={}", username);
        traineeService.delete(username, password);
    }

    public List<Trainer> getUnassignedTrainers(String username, String password) {
        log.info("Facade: getUnassignedTrainers username={}", username);
        return traineeService.getUnassignedTrainers(username, password);
    }

    public List<Trainer> getUnassignedActiveTrainers(String username, String password) {
        log.info("Facade: getUnassignedActiveTrainers username={}", username);
        return traineeService.getUnassignedActiveTrainers(username, password);
    }

    public Trainee updateTraineeTrainers(String username, String password, Collection<String> trainerUsernames) {
        log.info("Facade: updateTraineeTrainers username={}", username);
        return traineeService.updateTrainers(username, password, trainerUsernames);
    }

    // ---------- Training ----------

    public Training addTraining(String username, String password, TrainingRequest request) {
        log.info("Facade: addTraining by username={}", username);
        return trainingService.addTraining(username, password, request);
    }

    public Training addTraining(String username, String password, String traineeUsername, String trainerUsername,
                                String trainingName, LocalDate trainingDate, Integer durationMinutes) {
        log.info("Facade: addTraining (REST) by username={}", username);
        return trainingService.addTraining(username, password, traineeUsername, trainerUsername,
                trainingName, trainingDate, durationMinutes);
    }

    public List<Training> getTraineeTrainings(String username, String password, LocalDate fromDate,
                                              LocalDate toDate, String trainerName, String trainingTypeName) {
        log.info("Facade: getTraineeTrainings username={}", username);
        return trainingService.getTraineeTrainings(username, password, fromDate, toDate, trainerName, trainingTypeName);
    }

    public List<Training> getTrainerTrainings(String username, String password, LocalDate fromDate,
                                              LocalDate toDate, String traineeName) {
        log.info("Facade: getTrainerTrainings username={}", username);
        return trainingService.getTrainerTrainings(username, password, fromDate, toDate, traineeName);
    }

    // ---------- Auth / Training types (REST-only) ----------

    public void login(String username, String password) {
        log.info("Facade: login username={}", username);
        authenticationService.authenticate(username, password);
    }

    public void changeLogin(String username, String oldPassword, String newPassword) {
        log.info("Facade: changeLogin username={}", username);
        User user = authenticationService.authenticate(username, oldPassword);
        userAccountService.changePassword(user, newPassword);
    }

    public List<TrainingType> getTrainingTypes(String username, String password) {
        log.info("Facade: getTrainingTypes username={}", username);
        authenticationService.authenticate(username, password);
        return trainingTypeService.getAll();
    }
}