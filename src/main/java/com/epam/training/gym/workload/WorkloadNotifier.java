package com.epam.training.gym.workload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
public class WorkloadNotifier {

    static final String CIRCUIT_BREAKER_NAME = "trainerWorkload";
    private static final Logger log = LoggerFactory.getLogger(WorkloadNotifier.class);

    private final WorkloadClient client;
    private final CircuitBreaker circuitBreaker;

    public WorkloadNotifier(WorkloadClient client, CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.client = client;
        this.circuitBreaker = circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTrainingChanged(TrainingWorkloadEvent event) {
        TrainerWorkloadRequest request = event.request();
        Map<String, String> logContext = MDC.getCopyOfContextMap();
        circuitBreaker.run(() -> {
            withLogContext(logContext);
            client.updateWorkload(request);
            log.info("Workload {} sent for trainer={} date={} duration={}min", request.actionType(),
                    request.trainerUsername(), request.trainingDate(), request.trainingDuration());
            return true;
        }, failure -> {
            withLogContext(logContext);
            log.error("Workload {} for trainer={} date={} was NOT delivered ({}); needs to be resent",
                    request.actionType(), request.trainerUsername(), request.trainingDate(), failure.toString());
            return false;
        });
    }

    private static void withLogContext(Map<String, String> logContext) {
        if (logContext != null) {
            MDC.setContextMap(logContext);
        }
    }
}
