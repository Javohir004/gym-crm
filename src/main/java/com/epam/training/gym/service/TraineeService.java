package com.epam.training.gym.service;

import com.epam.training.gym.dao.TraineeDao;
import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.exception.NotFoundException;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.User;
import com.epam.training.gym.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@Transactional
public class TraineeService {

    private static final Logger log = LoggerFactory.getLogger(TraineeService.class);

    private TraineeDao traineeDao;
    private TrainerDao trainerDao;
    private AuthenticationService authenticationService;
    private UserAccountService userAccountService;

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    @Autowired
    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Autowired
    public void setUserAccountService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    public Trainee createTrainee(String firstName, String lastName, LocalDate dateOfBirth, String address) {
        User user = userAccountService.newUser(firstName, lastName);
        Trainee trainee = Trainee.builder()
                .user(user)
                .dateOfBirth(dateOfBirth)
                .address(address)
                .build();
        traineeDao.save(trainee);
        log.info("Trainee profile created: username={}", user.getUsername());
        return trainee;
    }

    @Transactional(readOnly = true)
    public boolean credentialsMatch(String username, String password) {
        return authenticationService.traineeCredentialsMatch(username, password);
    }

    @Transactional(readOnly = true)
    public Trainee getByUsername(String username, String password) {
        return authenticationService.authenticateTrainee(username, password);
    }

    public void changePassword(String username, String password, String newPassword) {
        Trainee trainee = authenticationService.authenticateTrainee(username, password);
        userAccountService.changePassword(trainee.getUser(), newPassword);
    }

    public Trainee update(String username, String password,
                          String firstName, String lastName, LocalDate dateOfBirth, String address) {
        Trainee trainee = authenticationService.authenticateTrainee(username, password);
        Validation.requireText(firstName, "First name");
        Validation.requireText(lastName, "Last name");

        trainee.getUser().setFirstName(firstName.trim());
        trainee.getUser().setLastName(lastName.trim());
        trainee.setDateOfBirth(dateOfBirth);
        trainee.setAddress(address);
        log.info("Updating trainee profile: username={}", username);
        return traineeDao.update(trainee);
    }

    public boolean toggleActive(String username, String password) {
        Trainee trainee = authenticationService.authenticateTrainee(username, password);
        return userAccountService.toggleActive(trainee.getUser());
    }

    public void delete(String username, String password) {
        Trainee trainee = authenticationService.authenticateTrainee(username, password);
        traineeDao.delete(trainee);
        log.info("Trainee profile deleted: username={}", username);
    }

    @Transactional(readOnly = true)
    public List<Trainer> getUnassignedTrainers(String username, String password) {
        authenticationService.authenticateTrainee(username, password);
        log.debug("Looking up trainers not assigned to trainee username={}", username);
        return trainerDao.findNotAssignedToTrainee(username);
    }

    public Trainee updateTrainers(String username, String password, Collection<String> trainerUsernames) {
        Trainee trainee = authenticationService.authenticateTrainee(username, password);
        Validation.requireNotNull(trainerUsernames, "Trainers list");

        Set<String> requested = new LinkedHashSet<>(trainerUsernames);
        List<Trainer> trainers = trainerDao.findAllByUsernames(requested);
        if (trainers.size() != requested.size()) {
            Set<String> found = trainers.stream()
                    .map(trainer -> trainer.getUser().getUsername())
                    .collect(Collectors.toSet());
            requested.removeAll(found);
            throw new NotFoundException("Trainers not found: " + requested);
        }

        trainee.getTrainers().clear();
        trainee.getTrainers().addAll(trainers);
        log.info("Updated trainers list of trainee username={}: {} trainer(s)", username, trainers.size());
        return traineeDao.update(trainee);
    }
}