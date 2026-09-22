package com.epam.training.gym.dao;

import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.storage.TraineeStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Repository
public class TraineeDao {

    private static final Logger log = LoggerFactory.getLogger(TraineeDao.class);

    private TraineeStorage storage;

    @Autowired
    public void setStorage(TraineeStorage storage) {
        this.storage = storage;
    }

    public Trainee create(Trainee trainee) {
        storage.save(trainee.getUserId(), trainee);
        log.info("Created trainee id={} username={}", trainee.getUserId(), trainee.getUsername());
        return trainee;
    }

    public Trainee update(Trainee trainee) {
        storage.save(trainee.getUserId(), trainee);
        log.info("Updated trainee id={}", trainee.getUserId());
        return trainee;
    }

    public void delete(Long id) {
        storage.delete(id);
        log.info("Deleted trainee id={}", id);
    }

    public Trainee select(Long id) {
        return storage.get(id);
    }

    public List<Trainee> selectAll() {
        return new ArrayList<>(storage.getAll());
    }

    public Set<String> getAllUsernames() {
        Set<String> usernames = new HashSet<>();
        for (Trainee trainee : storage.getAll()) {
            usernames.add(trainee.getUsername());
        }
        return usernames;
    }
}