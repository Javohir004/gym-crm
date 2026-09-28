package com.epam.training.gym.dao;

import com.epam.training.gym.model.Trainee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class TraineeDao {

    private static final Logger log = LoggerFactory.getLogger(TraineeDao.class);

    private EntityManager entityManager;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    public Trainee save(Trainee trainee) {
        entityManager.persist(trainee);
        log.info("Saved trainee id={} username={}", trainee.getId(), trainee.getUser().getUsername());
        return trainee;
    }

    public Trainee update(Trainee trainee) {
        Trainee merged = entityManager.merge(trainee);
        log.info("Updated trainee id={}", merged.getId());
        return merged;
    }

    public Optional<Trainee> findByUsername(String username) {
        return entityManager
                .createQuery("select t from Trainee t where t.user.username = :username", Trainee.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst();
    }


    public void delete(Trainee trainee) {
        Trainee managed = entityManager.contains(trainee) ? trainee : entityManager.merge(trainee);
        entityManager.remove(managed);
        log.info("Deleted trainee id={}", trainee.getId());
    }
}