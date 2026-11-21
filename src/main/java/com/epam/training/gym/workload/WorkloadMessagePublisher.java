package com.epam.training.gym.workload;

import com.epam.training.gym.logging.TransactionIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WorkloadMessagePublisher {

    public static final String TOKEN_PROPERTY = "serviceToken";
    public static final String TRANSACTION_ID_PROPERTY = "transactionId";
    public static final String GROUP_ID_PROPERTY = "JMSXGroupID";

    private static final Logger log = LoggerFactory.getLogger(WorkloadMessagePublisher.class);

    private final JmsTemplate jmsTemplate;
    private final ServiceTokenProvider tokenProvider;
    private final String queue;

    public WorkloadMessagePublisher(JmsTemplate jmsTemplate, ServiceTokenProvider tokenProvider,
                                    @Value("${gym.workload.jms.queue}") String queue) {
        this.jmsTemplate = jmsTemplate;
        this.tokenProvider = tokenProvider;
        this.queue = queue;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTrainingChanged(TrainingWorkloadEvent event) {
        TrainerWorkloadRequest request = event.request();
        String transactionId = MDC.get(TransactionIdFilter.MDC_KEY);
        try {
            jmsTemplate.convertAndSend(queue, request, message -> {
                message.setStringProperty(TOKEN_PROPERTY, tokenProvider.getToken());
                message.setStringProperty(GROUP_ID_PROPERTY, request.trainerUsername());
                if (transactionId != null) {
                    message.setStringProperty(TRANSACTION_ID_PROPERTY, transactionId);
                }
                return message;
            });
            log.info("Workload {} queued to '{}' for trainer={} date={} duration={}min", request.actionType(), queue,
                    request.trainerUsername(), request.trainingDate(), request.trainingDuration());
        } catch (JmsException e) {
            log.error("Workload {} for trainer={} date={} was NOT queued ({}); needs to be resent",
                    request.actionType(), request.trainerUsername(), request.trainingDate(), e.getMessage());
        }
    }
}
