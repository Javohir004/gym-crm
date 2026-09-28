package com.epam.training.gym.dao;

import com.epam.training.gym.model.Trainer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class TrainerDao {

    private static final Logger log = LoggerFactory.getLogger(TrainerDao.class);

    private EntityManager entityManager;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    public Trainer save(Trainer trainer) {
        entityManager.persist(trainer);
        log.info("Saved trainer id={} username={}", trainer.getId(), trainer.getUser().getUsername());
        return trainer;
    }

    public Trainer update(Trainer trainer) {
        Trainer merged = entityManager.merge(trainer);
        log.info("Updated trainer id={}", merged.getId());
        return merged;
    }

    public Optional<Trainer> findByUsername(String username) {
        return entityManager
                .createQuery("select t from Trainer t where t.user.username = :username", Trainer.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst();
    }

    public List<Trainer> findAllByUsernames(Collection<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return List.of();
        }
        return entityManager
                .createQuery("select t from Trainer t where t.user.username in :usernames", Trainer.class)
                .setParameter("usernames", usernames)
                .getResultList();
    }

    public List<Trainer> findNotAssignedToTrainee(String traineeUsername) {
        return entityManager
                .createQuery("select t from Trainer t where t.id not in "
                                + "(select a.id from Trainee tn join tn.trainers a where tn.user.username = :username)",
                        Trainer.class)
                .setParameter("username", traineeUsername)
                .getResultList();
    }
}