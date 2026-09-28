package com.epam.training.gym.service;

import com.epam.training.gym.dao.TraineeDao;
import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.dao.UserDao;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;


@Service
@Transactional(readOnly = true)
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private UserDao userDao;
    private TraineeDao traineeDao;
    private TrainerDao trainerDao;

    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    public User authenticate(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            throw failed(username);
        }
        User user = userDao.findByUsername(username)
                .filter(candidate -> passwordMatches(candidate, password))
                .orElseThrow(() -> failed(username));
        log.debug("Authenticated user username={}", username);
        return user;
    }

    public Trainee authenticateTrainee(String username, String password) {
        Trainee trainee = findTrainee(username, password).orElseThrow(() -> failed(username));
        log.debug("Authenticated trainee username={}", username);
        return trainee;
    }

    public Trainer authenticateTrainer(String username, String password) {
        Trainer trainer = findTrainer(username, password).orElseThrow(() -> failed(username));
        log.debug("Authenticated trainer username={}", username);
        return trainer;
    }

    public boolean traineeCredentialsMatch(String username, String password) {
        return findTrainee(username, password).isPresent();
    }

    public boolean trainerCredentialsMatch(String username, String password) {
        return findTrainer(username, password).isPresent();
    }

    private Optional<Trainee> findTrainee(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return Optional.empty();
        }
        return traineeDao.findByUsername(username)
                .filter(trainee -> passwordMatches(trainee.getUser(), password));
    }

    private Optional<Trainer> findTrainer(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return Optional.empty();
        }
        return trainerDao.findByUsername(username)
                .filter(trainer -> passwordMatches(trainer.getUser(), password));
    }

    private boolean passwordMatches(User user, String password) {
        return MessageDigest.isEqual(
                user.getPassword().getBytes(StandardCharsets.UTF_8),
                password.getBytes(StandardCharsets.UTF_8));
    }

    private AuthenticationException failed(String username) {
        log.warn("Authentication failed for username={}", username);
        return new AuthenticationException("Invalid username or password");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}