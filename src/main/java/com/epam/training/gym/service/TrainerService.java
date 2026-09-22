package com.epam.training.gym.service;

import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.model.Trainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Service
public class TrainerService {

    private static final Logger log = LoggerFactory.getLogger(TrainerService.class);

    private TrainerDao trainerDao;
    private UsernameGenerator usernameGenerator;
    private PasswordGenerator passwordGenerator;

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    @Autowired
    public void setUsernameGenerator(UsernameGenerator usernameGenerator) {
        this.usernameGenerator = usernameGenerator;
    }

    @Autowired
    public void setPasswordGenerator(PasswordGenerator passwordGenerator) {
        this.passwordGenerator = passwordGenerator;
    }

    public Trainer create(Trainer trainer) {
        if (trainer == null) {
            throw new IllegalArgumentException("Trainer must not be null");
        }
        log.info("Creating trainer profile for {} {}", trainer.getFirstName(), trainer.getLastName());

        trainer.setUserId(nextId());
        trainer.setUsername(usernameGenerator.generate(trainer.getFirstName(), trainer.getLastName()));
        trainer.setPassword(passwordGenerator.generate());
        trainer.setActive(true);

        Trainer created = trainerDao.create(trainer);
        log.info("Trainer profile created: id={}, username={}", created.getUserId(), created.getUsername());
        return created;
    }

    public Trainer update(Trainer trainer) {
        if (trainer == null || trainer.getUserId() == null) {
            throw new IllegalArgumentException("Trainer and its id must not be null");
        }
        if (trainerDao.select(trainer.getUserId()) == null) {
            log.warn("Cannot update trainer: no profile with id={}", trainer.getUserId());
            throw new IllegalArgumentException("Trainer not found: " + trainer.getUserId());
        }
        log.info("Updating trainer id={}", trainer.getUserId());
        return trainerDao.update(trainer);
    }

    public Optional<Trainer> select(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        Trainer trainer = trainerDao.select(id);
        if (trainer == null) {
            log.debug("No trainer found with id={}", id);
        }
        return Optional.ofNullable(trainer);
    }

    public List<Trainer> selectAll() {
        List<Trainer> all = trainerDao.selectAll();
        log.debug("Selected {} trainer(s)", all.size());
        return all;
    }

    private Long nextId() {
        return trainerDao.selectAll().stream()
                .map(Trainer::getUserId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1;
    }
}