package workload;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.logging.TransactionIdFilter;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import com.epam.training.gym.model.User;
import com.epam.training.gym.workload.ActionType;
import com.epam.training.gym.workload.ServiceTokenProvider;
import com.epam.training.gym.workload.TrainerWorkloadRequest;
import com.epam.training.gym.workload.TrainingWorkloadEvent;
import com.epam.training.gym.workload.WorkloadMessagePublisher;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import support.LogCapture;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkloadMessagePublisherTest {

    private static final TrainerWorkloadRequest REQUEST = new TrainerWorkloadRequest(
            "Anna.Lee", "Anna", "Lee", true, LocalDate.of(2026, 9, 1), 60, ActionType.ADD);

    @Mock
    private JmsTemplate jmsTemplate;
    @Mock
    private ServiceTokenProvider tokenProvider;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    private WorkloadMessagePublisher publisher() {
        return new WorkloadMessagePublisher(jmsTemplate, tokenProvider, "trainer.workload");
    }

    private MessagePostProcessor sentPostProcessor() {
        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(eq("trainer.workload"), eq(REQUEST), captor.capture());
        return captor.getValue();
    }

    @Test
    void sendsTheRequest_withToken_transactionId_andTrainerAsMessageGroup() throws JMSException {
        when(tokenProvider.getToken()).thenReturn("service-token");
        MDC.put(TransactionIdFilter.MDC_KEY, "tx-42");

        publisher().onTrainingChanged(new TrainingWorkloadEvent(REQUEST));

        Message message = mock(Message.class);
        sentPostProcessor().postProcessMessage(message);
        verify(message).setStringProperty(WorkloadMessagePublisher.TOKEN_PROPERTY, "service-token");
        verify(message).setStringProperty(WorkloadMessagePublisher.TRANSACTION_ID_PROPERTY, "tx-42");
        verify(message).setStringProperty(WorkloadMessagePublisher.GROUP_ID_PROPERTY, "Anna.Lee");
    }

    @Test
    void outsideARequest_sendsNoTransactionId() throws JMSException {
        when(tokenProvider.getToken()).thenReturn("service-token");

        publisher().onTrainingChanged(new TrainingWorkloadEvent(REQUEST));

        Message message = mock(Message.class);
        sentPostProcessor().postProcessMessage(message);
        verify(message, never()).setStringProperty(eq(WorkloadMessagePublisher.TRANSACTION_ID_PROPERTY), any());
    }

    @Test
    void whenTheBrokerIsDown_logsTheLostUpdate_andDoesNotThrow() {
        doThrow(new UncategorizedJmsException("Connection refused"))
                .when(jmsTemplate).convertAndSend(eq("trainer.workload"), eq(REQUEST), any(MessagePostProcessor.class));

        try (LogCapture logs = new LogCapture(WorkloadMessagePublisher.class)) {
            publisher().onTrainingChanged(new TrainingWorkloadEvent(REQUEST));

            assertTrue(logs.messages(Level.ERROR).stream()
                    .anyMatch(m -> m.contains("NOT queued") && m.contains("Anna.Lee")));
        }
    }

    @Test
    void requestIsBuiltFromTheTrainingsTrainer() {
        User user = User.builder().username("Anna.Lee").firstName("Anna").lastName("Lee").active(false).build();
        Training training = Training.builder().trainer(Trainer.builder().user(user).build())
                .trainingDate(LocalDate.of(2026, 9, 1)).trainingDuration(60).build();

        assertEquals(new TrainerWorkloadRequest("Anna.Lee", "Anna", "Lee", false, LocalDate.of(2026, 9, 1), 60,
                ActionType.DELETE), TrainerWorkloadRequest.of(training, ActionType.DELETE));
    }
}
