package workload;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import com.epam.training.gym.model.User;
import com.epam.training.gym.workload.ActionType;
import com.epam.training.gym.workload.TrainerWorkloadRequest;
import com.epam.training.gym.workload.TrainingWorkloadEvent;
import com.epam.training.gym.workload.WorkloadClient;
import com.epam.training.gym.workload.WorkloadNotifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ConfigBuilder;
import support.LogCapture;

import java.time.LocalDate;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkloadNotifierTest {

    private static final TrainerWorkloadRequest REQUEST = new TrainerWorkloadRequest(
            "Anna.Lee", "Anna", "Lee", true, LocalDate.of(2026, 9, 1), 60, ActionType.ADD);

    @Mock
    private WorkloadClient client;

    private static final class PassThroughFactory extends CircuitBreakerFactory<Object, ConfigBuilder<Object>> {

        @Override
        public CircuitBreaker create(String id) {
            return new CircuitBreaker() {
                @Override
                public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
                    try {
                        return toRun.get();
                    } catch (RuntimeException e) {
                        return fallback.apply(e);
                    }
                }
            };
        }

        @Override
        protected ConfigBuilder<Object> configBuilder(String id) {
            return null;
        }

        @Override
        public void configureDefault(Function<String, Object> defaultConfiguration) {
        }
    }

    @Test
    void sendsTheRequestToTheWorkloadService() {
        new WorkloadNotifier(client, new PassThroughFactory()).onTrainingChanged(new TrainingWorkloadEvent(REQUEST));

        verify(client).updateWorkload(REQUEST);
    }

    @Test
    void whenTheServiceFails_fallbackLogsTheLostUpdate_andDoesNotThrow() {
        doThrow(new IllegalStateException("connection refused")).when(client).updateWorkload(REQUEST);

        try (LogCapture logs = new LogCapture(WorkloadNotifier.class)) {
            new WorkloadNotifier(client, new PassThroughFactory())
                    .onTrainingChanged(new TrainingWorkloadEvent(REQUEST));

            assertTrue(logs.messages(Level.ERROR).stream()
                    .anyMatch(m -> m.contains("NOT delivered") && m.contains("Anna.Lee")));
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
