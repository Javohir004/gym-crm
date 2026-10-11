package com.epam.training.gym.dao;

import com.epam.training.gym.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public class UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDao.class);

    private EntityManager entityManager;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<User> findByUsername(String username) {
        return entityManager
                .createQuery("select u from User u where u.username = :username", User.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst();
    }


    public List<String> findUsernamesStartingWith(String prefix) {
        return entityManager
                .createQuery("select u.username from User u where u.username like :prefix", String.class)
                .setParameter("prefix", prefix + "%")
                .getResultList();
    }

    public User update(User user) {
        User merged = entityManager.merge(user);
        log.info("Updated user id={} username={}", merged.getId(), merged.getUsername());
        return merged;
    }
}