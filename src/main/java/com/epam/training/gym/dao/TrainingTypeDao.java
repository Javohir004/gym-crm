package com.epam.training.gym.dao;

import com.epam.training.gym.model.TrainingType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public class TrainingTypeDao {

    private EntityManager entityManager;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<TrainingType> findByName(String name) {
        return entityManager
                .createQuery("select tt from TrainingType tt where tt.trainingTypeName = :name", TrainingType.class)
                .setParameter("name", name)
                .getResultStream()
                .findFirst();
    }

    public List<TrainingType> findAll() {
        return entityManager
                .createQuery("select tt from TrainingType tt order by tt.id", TrainingType.class)
                .getResultList();
    }
}