package com.epam.training.gym.facade;

import com.epam.training.gym.dto.TrainingRequest;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import com.epam.training.gym.service.TraineeService;
import com.epam.training.gym.service.TrainerService;
import com.epam.training.gym.service.TrainingService;
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

    public GymFacade(TraineeService traineeService,
                     TrainerService trainerService,
                     TrainingService trainingService) {
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
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

    /** 9 */
    public Trainer updateTrainer(String username, String password,
                                 String firstName, String lastName, String specialization) {
        log.info("Facade: updateTrainer username={}", username);
        return trainerService.update(username, password, firstName, lastName, specialization);
    }

    /** 12 */
    public boolean toggleTrainerActive(String username, String password) {
        log.info("Facade: toggleTrainerActive username={}", username);
        return trainerService.toggleActive(username, password);
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

    /** 10 */
    public Trainee updateTrainee(String username, String password,
                                 String firstName, String lastName, LocalDate dateOfBirth, String address) {
        log.info("Facade: updateTrainee username={}", username);
        return traineeService.update(username, password, firstName, lastName, dateOfBirth, address);
    }

    /** 11 */
    public boolean toggleTraineeActive(String username, String password) {
        log.info("Facade: toggleTraineeActive username={}", username);
        return traineeService.toggleActive(username, password);
    }

    /** 13 */
    public void deleteTrainee(String username, String password) {
        log.info("Facade: deleteTrainee username={}", username);
        traineeService.delete(username, password);
    }

    /** 17 */
    public List<Trainer> getUnassignedTrainers(String username, String password) {
        log.info("Facade: getUnassignedTrainers username={}", username);
        return traineeService.getUnassignedTrainers(username, password);
    }

    /** 18 */
    public Trainee updateTraineeTrainers(String username, String password, Collection<String> trainerUsernames) {
        log.info("Facade: updateTraineeTrainers username={}", username);
        return traineeService.updateTrainers(username, password, trainerUsernames);
    }

    // ---------- Training ----------

    /** 16 */
    public Training addTraining(String username, String password, TrainingRequest request) {
        log.info("Facade: addTraining by username={}", username);
        return trainingService.addTraining(username, password, request);
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
}