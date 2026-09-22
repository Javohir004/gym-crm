package com.epam.training.gym.dao;

import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.storage.TrainerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Repository
public class TrainerDao {

    private static final Logger log = LoggerFactory.getLogger(TrainerDao.class);

    private TrainerStorage storage;

    @Autowired
    public void setStorage(TrainerStorage storage) {
        this.storage = storage;
    }

    public Trainer create(Trainer trainer) {
        storage.save(trainer.getUserId(), trainer);
        log.info("Created trainer id={} username={}", trainer.getUserId(), trainer.getUsername());
        return trainer;
    }

    public Trainer update(Trainer trainer) {
        storage.save(trainer.getUserId(), trainer);
        log.info("Updated trainer id={}", trainer.getUserId());
        return trainer;
    }

    public Trainer select(Long id) {
        return storage.get(id);
    }

    public List<Trainer> selectAll() {
        return new ArrayList<>(storage.getAll());
    }


    public Set<String> getAllUsernames() {
        Set<String> usernames = new HashSet<>();
        for (Trainer trainer : storage.getAll()) {
            usernames.add(trainer.getUsername());
        }
        return usernames;
    }
}