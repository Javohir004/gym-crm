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

/**
 * Single entry point into the gym CRM: one method per required function
 * (Hibernate module's 18 + REST module's extra ones). Services are injected
 * through the constructor, as required by the task. Passwords are never
 * logged.
 */
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

    /** 1 */
    public Trainer createTrainer(String firstName, String lastName, String specialization) {
        log.info("Facade: createTrainer");
        return trainerService.createTrainer(firstName, lastName, specialization);
    }

    /** 4 */
    public boolean trainerCredentialsMatch(String username, String password) {
        log.info("Facade: trainerCredentialsMatch username={}", username);
        return trainerService.credentialsMatch(username, password);
    }

    /** 5 */
    public Trainer getTrainer(String username, String password) {
        log.info("Facade: getTrainer username={}", username);
        return trainerService.getByUsername(username, password);
    }

    /** 8 */
    public void changeTrainerPassword(String username, String password, String newPassword) {
        log.info("Facade: changeTrainerPassword username={}", username);
        trainerService.changePassword(username, password, newPassword);
    }

    /** 9 (Hibernate module version: lets specialization be changed too) */
    public Trainer updateTrainer(String username, String password,
                                 String firstName, String lastName, String specialization) {
        log.info("Facade: updateTrainer username={}", username);
        return trainerService.update(username, password, firstName, lastName, specialization);
    }

    /** REST "Update Trainer Profile": specialization stays read-only, Is Active is explicit. */
    public Trainer updateTrainerProfile(String username, String password,
                                        String firstName, String lastName, boolean isActive) {
        log.info("Facade: updateTrainerProfile username={}", username);
        return trainerService.updateFullProfile(username, password, firstName, lastName, isActive);
    }

    /** 12 (Hibernate module version: flips the current state) */
    public boolean toggleTrainerActive(String username, String password) {
        log.info("Facade: toggleTrainerActive username={}", username);
        return trainerService.toggleActive(username, password);
    }

    /** REST "Activate/De-activate Trainer": sets an explicit value. */
    public void setTrainerActive(String username, String password, boolean active) {
        log.info("Facade: setTrainerActive username={} active={}", username, active);
        trainerService.setActive(username, password, active);
    }

    // ---------- Trainee ----------

    /** 2 */
    public Trainee createTrainee(String firstName, String lastName, LocalDate dateOfBirth, String address) {
        log.info("Facade: createTrainee");
        return traineeService.createTrainee(firstName, lastName, dateOfBirth, address);
    }

    /** 3 */
    public boolean traineeCredentialsMatch(String username, String password) {
        log.info("Facade: traineeCredentialsMatch username={}", username);
        return traineeService.credentialsMatch(username, password);
    }

    /** 6 */
    public Trainee getTrainee(String username, String password) {
        log.info("Facade: getTrainee username={}", username);
        return traineeService.getByUsername(username, password);
    }

    /** 7 */
    public void changeTraineePassword(String username, String password, String newPassword) {
        log.info("Facade: changeTraineePassword username={}", username);
        traineeService.changePassword(username, password, newPassword);
    }

    /** 10 (Hibernate module version) */
    public Trainee updateTrainee(String username, String password,
                                 String firstName, String lastName, LocalDate dateOfBirth, String address) {
        log.info("Facade: updateTrainee username={}", username);
        return traineeService.update(username, password, firstName, lastName, dateOfBirth, address);
    }

    /** REST "Update Trainee Profile": Is Active is explicit. */
    public Trainee updateTraineeProfile(String username, String password, String firstName, String lastName,
                                        LocalDate dateOfBirth, String address, boolean isActive) {
        log.info("Facade: updateTraineeProfile username={}", username);
        return traineeService.updateFullProfile(username, password, firstName, lastName, dateOfBirth, address, isActive);
    }

    /** 11 (Hibernate module version: flips the current state) */
    public boolean toggleTraineeActive(String username, String password) {
        log.info("Facade: toggleTraineeActive username={}", username);
        return traineeService.toggleActive(username, password);
    }

    /** REST "Activate/De-activate Trainee": sets an explicit value. */
    public void setTraineeActive(String username, String password, boolean active) {
        log.info("Facade: setTraineeActive username={} active={}", username, active);
        traineeService.setActive(username, password, active);
    }

    /** 13 */
    public void deleteTrainee(String username, String password) {
        log.info("Facade: deleteTrainee username={}", username);
        traineeService.delete(username, password);
    }

    /** 17 (Hibernate module version: any status) */
    public List<Trainer> getUnassignedTrainers(String username, String password) {
        log.info("Facade: getUnassignedTrainers username={}", username);
        return traineeService.getUnassignedTrainers(username, password);
    }

    /** REST "Get not assigned active trainers" (note 10): active only. */
    public List<Trainer> getUnassignedActiveTrainers(String username, String password) {
        log.info("Facade: getUnassignedActiveTrainers username={}", username);
        return traineeService.getUnassignedActiveTrainers(username, password);
    }

    /** 18 */
    public Trainee updateTraineeTrainers(String username, String password, Collection<String> trainerUsernames) {
        log.info("Facade: updateTraineeTrainers username={}", username);
        return traineeService.updateTrainers(username, password, trainerUsernames);
    }

    // ---------- Training ----------

    /** 16 (Hibernate module version: request carries an explicit Training Type) */
    public Training addTraining(String username, String password, TrainingRequest request) {
        log.info("Facade: addTraining by username={}", username);
        return trainingService.addTraining(username, password, request);
    }

    /** REST "Add Training": type comes from the trainer's own specialization. */
    public Training addTraining(String username, String password, String traineeUsername, String trainerUsername,
                                String trainingName, LocalDate trainingDate, Integer durationMinutes) {
        log.info("Facade: addTraining (REST) by username={}", username);
        return trainingService.addTraining(username, password, traineeUsername, trainerUsername,
                trainingName, trainingDate, durationMinutes);
    }

    /** 14 */
    public List<Training> getTraineeTrainings(String username, String password, LocalDate fromDate,
                                              LocalDate toDate, String trainerName, String trainingTypeName) {
        log.info("Facade: getTraineeTrainings username={}", username);
        return trainingService.getTraineeTrainings(username, password, fromDate, toDate, trainerName, trainingTypeName);
    }

    /** 15 */
    public List<Training> getTrainerTrainings(String username, String password, LocalDate fromDate,
                                              LocalDate toDate, String traineeName) {
        log.info("Facade: getTrainerTrainings username={}", username);
        return trainingService.getTrainerTrainings(username, password, fromDate, toDate, traineeName);
    }

    // ---------- Auth / Training types (REST-only) ----------

    /** REST "Login": throws AuthenticationException if the pair does not match. */
    public void login(String username, String password) {
        log.info("Facade: login username={}", username);
        authenticationService.authenticate(username, password);
    }

    /** REST "Change Login": works for a Trainee or a Trainer alike. */
    public void changeLogin(String username, String oldPassword, String newPassword) {
        log.info("Facade: changeLogin username={}", username);
        User user = authenticationService.authenticate(username, oldPassword);
        userAccountService.changePassword(user, newPassword);
    }

    /** REST "Get Training types". */
    public List<TrainingType> getTrainingTypes() {
        log.info("Facade: getTrainingTypes");
        return trainingTypeService.getAll();
    }
}