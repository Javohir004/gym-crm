package com.epam.training.gym.health;

import com.epam.training.gym.dao.TrainerDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;


@Component
public class TrainerAvailabilityHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(TrainerAvailabilityHealthIndicator.class);

    private final TrainerDao trainerDao;

    public TrainerAvailabilityHealthIndicator(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    @Override
    public Health health() {
        try {
            long total = trainerDao.countAll();
            long active = trainerDao.countActive();

            Health.Builder builder = (total > 0 && active == 0) ? Health.unknown() : Health.up();
            if (total > 0 && active == 0) {
                log.warn("Health warning: all {} trainer(s) are inactive", total);
                builder.withDetail("warning", "All trainers are inactive");
            }
            return builder
                    .withDetail("totalTrainers", total)
                    .withDetail("activeTrainers", active)
                    .withDetail("inactiveTrainers", total - active)
                    .build();
        } catch (Exception e) {
            log.error("Health check failed: cannot count trainers", e);
            return Health.down(e).build();
        }
    }
}