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
    public void setUserAccountService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    public Registration<Trainer> createTrainer(String firstName, String lastName, String specializationName) {
        Validation.requireText(specializationName, "Specialization");
        TrainingType specialization = findTrainingType(specializationName);

        Registration<User> account = userAccountService.newUser(firstName, lastName);
        User user = account.profile();
        Trainer trainer = Trainer.builder()
                .user(user)
                .specialization(specialization)
                .build();
        trainerDao.save(trainer);
        log.info("Trainer profile created: username={}", user.getUsername());
        return new Registration<>(trainer, account.rawPassword());
    }

    @Transactional(readOnly = true)
    public Trainer getByUsername(String username) {
        return findTrainer(username);
    }

    public void setActive(String username, boolean active) {
        Trainer trainer = findTrainer(username);
        userAccountService.setActive(trainer.getUser(), active);
    }


    public Trainer updateFullProfile(String username, String firstName, String lastName, boolean isActive) {
        Trainer trainer = findTrainer(username);
        Validation.requireText(firstName, "First name");
        Validation.requireText(lastName, "Last name");

        trainer.getUser().setFirstName(firstName.trim());
        trainer.getUser().setLastName(lastName.trim());
        trainer.getUser().setActive(isActive);
        log.info("Updating trainer full profile: username={}", username);
        return trainerDao.update(trainer);
    }

    private Trainer findTrainer(String username) {
        return trainerDao.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Trainer not found: " + username));
    }

    private TrainingType findTrainingType(String name) {
        return trainingTypeDao.findByName(name.trim())
                .orElseThrow(() -> new NotFoundException("Training type not found: " + name));
    }
}