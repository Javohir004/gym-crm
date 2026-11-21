package com.epam.training.gym.health;

import com.epam.training.gym.service.TrainingTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class TrainingTypesHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(TrainingTypesHealthIndicator.class);

    private final TrainingTypeService trainingTypeService;

    public TrainingTypesHealthIndicator(TrainingTypeService trainingTypeService) {
        this.trainingTypeService = trainingTypeService;
    }

    @Override
    public Health health() {
        try {
            int count = trainingTypeService.getAll().size();
            if (count == 0) {
                log.error("Health check failed: training_type table is empty");
                return Health.down().withDetail("reason", "No training types found").build();
            }
            log.debug("Training types health check OK: {} type(s)", count);
            return Health.up().withDetail("trainingTypes", count).build();
        } catch (Exception e) {
            log.error("Health check failed: cannot read training types", e);
            return Health.down(e).build();
        }
    }
}