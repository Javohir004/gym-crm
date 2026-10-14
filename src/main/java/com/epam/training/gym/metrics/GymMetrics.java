package com.epam.training.gym.metrics;

import com.epam.training.gym.dao.TrainerDao;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;


@Component
public class GymMetrics {

    private final Counter traineeRegistrations;
    private final Counter trainerRegistrations;
    private final Counter trainingsAdded;
    private final Counter authenticationFailures;

    public GymMetrics(MeterRegistry registry, TrainerDao trainerDao) {
        this.traineeRegistrations = Counter.builder("gym.registrations")
                .description("Number of registered profiles")
                .tag("role", "trainee")
                .register(registry);
        this.trainerRegistrations = Counter.builder("gym.registrations")
                .description("Number of registered profiles")
                .tag("role", "trainer")
                .register(registry);
        this.trainingsAdded = Counter.builder("gym.trainings.added")
                .description("Number of trainings added")
                .register(registry);
        this.authenticationFailures = Counter.builder("gym.authentication.failures")
                .description("Number of failed authentications")
                .register(registry);

        Gauge.builder("gym.trainers.active", trainerDao, TrainerDao::countActive)
                .description("Current number of active trainers")
                .register(registry);
    }

    public void traineeRegistered() {
        traineeRegistrations.increment();
    }

    public void trainerRegistered() {
        trainerRegistrations.increment();
    }

    public void trainingAdded() {
        trainingsAdded.increment();
    }

    public void authenticationFailed() {
        authenticationFailures.increment();
    }
}