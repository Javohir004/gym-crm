package com.epam.training.gym.service;

import com.epam.training.gym.dao.TraineeDao;
import com.epam.training.gym.model.Trainee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class TraineeService {

    private static final Logger log = LoggerFactory.getLogger(TraineeService.class);

    private TraineeDao traineeDao;
    private UsernameGenerator usernameGenerator;
    private PasswordGenerator passwordGenerator;

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setUsernameGenerator(UsernameGenerator usernameGenerator) {
        this.usernameGenerator = usernameGenerator;
    }

    @Autowired
    public void setPasswordGenerator(PasswordGenerator passwordGenerator) {
        this.passwordGenerator = passwordGenerator;
    }


    public Trainee create(Trainee trainee) {
        if (trainee == null) {
            throw new IllegalArgumentException("Trainee must not be null");
        }
        log.info("Creating trainee profile for {} {}", trainee.getFirstName(), trainee.getLastName());

        trainee.setUserId(nextId());
        trainee.setUsername(usernameGenerator.generate(trainee.getFirstName(), trainee.getLastName()));
        trainee.setPassword(passwordGenerator.generate());
        trainee.setActive(true);

        Trainee created = traineeDao.create(trainee);
        log.info("Trainee profile created: id={}, username={}", created.getUserId(), created.getUsername());
        return created;
    }

    public Trainee update(Trainee trainee) {
        if (trainee == null || trainee.getUserId() == null) {
            throw new IllegalArgumentException("Trainee and its id must not be null");
        }
        if (traineeDao.select(trainee.getUserId()) == null) {
            log.warn("Cannot update trainee: no profile with id={}", trainee.getUserId());
            throw new IllegalArgumentException("Trainee not found: " + trainee.getUserId());
        }
        log.info("Updating trainee id={}", trainee.getUserId());
        return traineeDao.update(trainee);
    }

    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }
        if (traineeDao.select(id) == null) {
            log.warn("Cannot delete trainee: no profile with id={}", id);
            return;
        }
        traineeDao.delete(id);
        log.info("Trainee profile deleted: id={}", id);
    }

    public Optional<Trainee> select(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        Trainee trainee = traineeDao.select(id);
        if (trainee == null) {
            log.debug("No trainee found with id={}", id);
        }
        return Optional.ofNullable(trainee);
    }

    public List<Trainee> selectAll() {
        List<Trainee> all = traineeDao.selectAll();
        log.debug("Selected {} trainee(s)", all.size());
        return all;
    }

    private Long nextId() {
        return traineeDao.selectAll().stream()
                .map(Trainee::getUserId)
                .filter(java.util.Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1;
    }
}