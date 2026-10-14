package com.epam.training.gym.metrics;

import com.epam.training.gym.exception.AuthenticationException;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Aspect
@Component
@Order(0)
public class GymMetricsAspect {

    private final GymMetrics metrics;

    public GymMetricsAspect(GymMetrics metrics) {
        this.metrics = metrics;
    }

    @AfterReturning("execution(* com.epam.training.gym.service.TraineeService.createTrainee(..))")
    public void afterTraineeCreated() {
        metrics.traineeRegistered();
    }

    @AfterReturning("execution(* com.epam.training.gym.service.TrainerService.createTrainer(..))")
    public void afterTrainerCreated() {
        metrics.trainerRegistered();
    }

    @AfterReturning("execution(* com.epam.training.gym.service.TrainingService.addTraining(..))")
    public void afterTrainingAdded() {
        metrics.trainingAdded();
    }

    @AfterThrowing(
            pointcut = "execution(* com.epam.training.gym.service.AuthenticationService.authenticate*(..))",
            throwing = "ex")
    public void afterAuthenticationFailed(AuthenticationException ex) {
        metrics.authenticationFailed();
    }
}