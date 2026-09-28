package com.epam.training.gym.dao;

import com.epam.training.gym.model.Training;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TrainingDao {

    private static final Logger log = LoggerFactory.getLogger(TrainingDao.class);

    private EntityManager entityManager;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Training save(Training training) {
        entityManager.persist(training);
        log.info("Saved training id={} name='{}'", training.getId(), training.getTrainingName());
        return training;
    }


    public List<Training> findByTraineeUsername(String traineeUsername, LocalDate fromDate, LocalDate toDate,
                                                String trainerName, String trainingTypeName) {
        StringBuilder jpql = new StringBuilder("select t from Training t where t.trainee.user.username = :username");
        Map<String, Object> params = new HashMap<>();
        params.put("username", traineeUsername);

        addDateRange(jpql, params, fromDate, toDate);
        if (hasText(trainerName)) {
            jpql.append(" and lower(concat(t.trainer.user.firstName, ' ', t.trainer.user.lastName)) like :personName");
            params.put("personName", "%" + trainerName.trim().toLowerCase() + "%");
        }
        if (hasText(trainingTypeName)) {
            jpql.append(" and t.trainingType.trainingTypeName = :typeName");
            params.put("typeName", trainingTypeName.trim());
        }
        return execute(jpql, params);
    }

    public List<Training> findByTrainerUsername(String trainerUsername, LocalDate fromDate, LocalDate toDate,
                                                String traineeName) {
        StringBuilder jpql = new StringBuilder("select t from Training t where t.trainer.user.username = :username");
        Map<String, Object> params = new HashMap<>();
        params.put("username", trainerUsername);

        addDateRange(jpql, params, fromDate, toDate);
        if (hasText(traineeName)) {
            jpql.append(" and lower(concat(t.trainee.user.firstName, ' ', t.trainee.user.lastName)) like :personName");
            params.put("personName", "%" + traineeName.trim().toLowerCase() + "%");
        }
        return execute(jpql, params);
    }

    private void addDateRange(StringBuilder jpql, Map<String, Object> params, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null) {
            jpql.append(" and t.trainingDate >= :fromDate");
            params.put("fromDate", fromDate);
        }
        if (toDate != null) {
            jpql.append(" and t.trainingDate <= :toDate");
            params.put("toDate", toDate);
        }
    }

    private List<Training> execute(StringBuilder jpql, Map<String, Object> params) {
        jpql.append(" order by t.trainingDate");
        TypedQuery<Training> query = entityManager.createQuery(jpql.toString(), Training.class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}