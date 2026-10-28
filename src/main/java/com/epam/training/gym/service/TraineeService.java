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
    public void setUserAccountService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    public Registration<Trainee> createTrainee(String firstName, String lastName, LocalDate dateOfBirth, String address) {
        Registration<User> account = userAccountService.newUser(firstName, lastName);
        User user = account.profile();
        Trainee trainee = Trainee.builder()
                .user(user)
                .dateOfBirth(dateOfBirth)
                .address(address)
                .build();
        traineeDao.save(trainee);
        log.info("Trainee profile created: username={}", user.getUsername());
        return new Registration<>(trainee, account.rawPassword());
    }

    @Transactional(readOnly = true)
    public Trainee getByUsername(String username) {
        return findTrainee(username);
    }

    public void setActive(String username, boolean active) {
        Trainee trainee = findTrainee(username);
        userAccountService.setActive(trainee.getUser(), active);
    }


    public Trainee updateFullProfile(String username, String firstName, String lastName,
                                     LocalDate dateOfBirth, String address, boolean isActive) {
        Trainee trainee = findTrainee(username);
        Validation.requireText(firstName, "First name");
        Validation.requireText(lastName, "Last name");

        trainee.getUser().setFirstName(firstName.trim());
        trainee.getUser().setLastName(lastName.trim());
        trainee.getUser().setActive(isActive);
        trainee.setDateOfBirth(dateOfBirth);
        trainee.setAddress(address);
        log.info("Updating trainee full profile: username={}", username);
        return traineeDao.update(trainee);
    }

    @Transactional(readOnly = true)
    public List<Trainer> getUnassignedActiveTrainers(String username) {
        findTrainee(username);
        log.debug("Looking up active trainers not assigned to trainee username={}", username);
        return trainerDao.findNotAssignedToTraineeActive(username);
    }

    public void delete(String username) {
        Trainee trainee = findTrainee(username);
        traineeDao.delete(trainee);
        log.info("Trainee profile deleted: username={}", username);
    }

    public Trainee updateTrainers(String username, Collection<String> trainerUsernames) {
        Trainee trainee = findTrainee(username);
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

    private Trainee findTrainee(String username) {
        return traineeDao.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Trainee not found: " + username));
    }
}