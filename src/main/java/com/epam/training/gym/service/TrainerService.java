package com.epam.training.gym.service;

import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.dao.TrainingTypeDao;
import com.epam.training.gym.exception.NotFoundException;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.TrainingType;
import com.epam.training.gym.model.User;
import com.epam.training.gym.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class TrainerService {

    private static final Logger log = LoggerFactory.getLogger(TrainerService.class);

    private TrainerDao trainerDao;
    private TrainingTypeDao trainingTypeDao;
    private AuthenticationService authenticationService;
    private UserAccountService userAccountService;

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

    @Autowired
    public void setUserAccountService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    public Trainer createTrainer(String firstName, String lastName, String specializationName) {
        Validation.requireText(specializationName, "Specialization");
        TrainingType specialization = findTrainingType(specializationName);

        User user = userAccountService.newUser(firstName, lastName);
        Trainer trainer = Trainer.builder()
                .user(user)
                .specialization(specialization)
                .build();
        trainerDao.save(trainer);
        log.info("Trainer profile created: username={}", user.getUsername());
        return trainer;
    }

    @Transactional(readOnly = true)
    public boolean credentialsMatch(String username, String password) {
        return authenticationService.trainerCredentialsMatch(username, password);
    }

    @Transactional(readOnly = true)
    public Trainer getByUsername(String username, String password) {
        return authenticationService.authenticateTrainer(username, password);
    }

    public void changePassword(String username, String password, String newPassword) {
        Trainer trainer = authenticationService.authenticateTrainer(username, password);
        userAccountService.changePassword(trainer.getUser(), newPassword);
    }
    public Trainer update(String username, String password,
                          String firstName, String lastName, String specializationName) {
        Trainer trainer = authenticationService.authenticateTrainer(username, password);
        Validation.requireText(firstName, "First name");
        Validation.requireText(lastName, "Last name");
        Validation.requireText(specializationName, "Specialization");

        trainer.getUser().setFirstName(firstName.trim());
        trainer.getUser().setLastName(lastName.trim());
        trainer.setSpecialization(findTrainingType(specializationName));
        log.info("Updating trainer profile: username={}", username);
        return trainerDao.update(trainer);
    }

    public boolean toggleActive(String username, String password) {
        Trainer trainer = authenticationService.authenticateTrainer(username, password);
        return userAccountService.toggleActive(trainer.getUser());
    }

    private TrainingType findTrainingType(String name) {
        return trainingTypeDao.findByName(name.trim())
                .orElseThrow(() -> new NotFoundException("Training type not found: " + name));
    }
}