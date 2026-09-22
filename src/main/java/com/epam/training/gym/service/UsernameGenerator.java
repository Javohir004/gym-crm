package com.epam.training.gym.service;

import com.epam.training.gym.dao.TraineeDao;
import com.epam.training.gym.dao.TrainerDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;


@Component
public class UsernameGenerator {

    private static final Logger log = LoggerFactory.getLogger(UsernameGenerator.class);

    private TraineeDao traineeDao;
    private TrainerDao trainerDao;

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    public String generate(String firstName, String lastName) {
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("First name and last name must not be empty");
        }

        String base = firstName.trim() + "." + lastName.trim();

        Set<String> taken = new HashSet<>();
        taken.addAll(traineeDao.getAllUsernames());
        taken.addAll(trainerDao.getAllUsernames());

        if (!taken.contains(base)) {
            log.debug("Generated username '{}'", base);
            return base;
        }

        int serial = 1;
        while (taken.contains(base + serial)) {
            serial++;
        }
        String result = base + serial;
        log.debug("Username '{}' already taken, generated '{}'", base, result);
        return result;
    }
}