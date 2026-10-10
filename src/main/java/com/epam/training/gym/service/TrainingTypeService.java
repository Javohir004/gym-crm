package com.epam.training.gym.service;

import com.epam.training.gym.dao.TrainingTypeDao;
import com.epam.training.gym.model.TrainingType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only: Training Types are a constant list (task note 14 / REST note
 * 14), so this service only exposes a way to list them.
 */
@Service
@Transactional(readOnly = true)
public class TrainingTypeService {

    private TrainingTypeDao trainingTypeDao;

    @Autowired
    public void setTrainingTypeDao(TrainingTypeDao trainingTypeDao) {
        this.trainingTypeDao = trainingTypeDao;
    }

    public List<TrainingType> getAll() {
        return trainingTypeDao.findAll();
    }
}