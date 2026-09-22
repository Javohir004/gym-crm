package com.epam.training.gym.dao;

import com.epam.training.gym.model.Training;
import com.epam.training.gym.storage.TrainingStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;


@Repository
public class TrainingDao {

    private static final Logger log = LoggerFactory.getLogger(TrainingDao.class);

    private TrainingStorage storage;

    @Autowired
    public void setStorage(TrainingStorage storage) {
        this.storage = storage;
    }

    public Training create(Training training) {
        storage.save(training.getTrainingId(), training);
        log.info("Created training id={} name='{}'", training.getTrainingId(), training.getTrainingName());
        return training;
    }

    public Training select(Long id) {
        return storage.get(id);
    }

    public List<Training> selectAll() {
        return new ArrayList<>(storage.getAll());
    }
}