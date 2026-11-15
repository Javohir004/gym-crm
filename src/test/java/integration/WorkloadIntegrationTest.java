package integration;

import com.epam.training.gym.GymCrmApplication;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.service.TraineeService;
import com.epam.training.gym.service.TrainerService;
import com.epam.training.gym.service.TrainingService;
import com.epam.training.gym.workload.ActionType;
import com.epam.training.gym.workload.TrainerWorkloadRequest;
import com.epam.training.gym.workload.TrainingWorkloadEvent;
import com.epam.training.gym.workload.WorkloadClient;
import com.epam.training.gym.workload.WorkloadNotifier;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = GymCrmApplication.class,
        properties = "spring.datasource.url=jdbc:h2:mem:workload-test;DB_CLOSE_DELAY=-1")
@ActiveProfiles("local")
class WorkloadIntegrationTest {

    @MockBean
    private WorkloadClient workloadClient;

    @Autowired
    private TraineeService traineeService;
    @Autowired
    private TrainerService trainerService;
    @Autowired
    private TrainingService trainingService;
    @Autowired
    private WorkloadNotifier notifier;
    @Autowired
    private Resilience4JCircuitBreakerFactory circuitBreakerFactory;

    private CircuitBreaker breaker() {
        return circuitBreakerFactory.getCircuitBreakerRegistry().circuitBreaker("trainerWorkload");
    }

    @BeforeEach
    void closeBreaker() {
        breaker().reset();
    }

    @Test
    void addingTraining_sendsAdd_andDeletingTrainee_cancelsOnlyUpcomingTrainings() {
        Trainee trainee = traineeService.createTrainee("Work", "Load", null, null).profile();
        Trainer trainer = trainerService.createTrainer("Coach", "Load", "Yoga").profile();
        String traineeName = trainee.getUser().getUsername();
        String trainerName = trainer.getUser().getUsername();
        LocalDate past = LocalDate.now().minusDays(10);
        LocalDate upcoming = LocalDate.now().plusDays(10);

        trainingService.addTraining(traineeName, trainerName, "Past session", past, 30);
        trainingService.addTraining(traineeName, trainerName, "Next session", upcoming, 60);
        traineeService.delete(traineeName);

        verify(workloadClient).updateWorkload(
                new TrainerWorkloadRequest(trainerName, "Coach", "Load", true, past, 30, ActionType.ADD));
        verify(workloadClient).updateWorkload(
                new TrainerWorkloadRequest(trainerName, "Coach", "Load", true, upcoming, 60, ActionType.ADD));
        verify(workloadClient).updateWorkload(
                new TrainerWorkloadRequest(trainerName, "Coach", "Load", true, upcoming, 60, ActionType.DELETE));
        verify(workloadClient, times(3)).updateWorkload(any());
    }

    @Test
    void failedTransaction_sendsNothing() {
        try {
            trainingService.addTraining("No.Trainee", "No.Trainer", "Ghost", LocalDate.now(), 30);
        } catch (RuntimeException expected) {
        }

        verify(workloadClient, times(0)).updateWorkload(any());
    }

    @Test
    void circuitBreaker_opensAfterRepeatedFailures_andStopsCallingTheService() {
        doThrow(new IllegalStateException("trainer-workload-service is down"))
                .when(workloadClient).updateWorkload(any());
        TrainingWorkloadEvent event = new TrainingWorkloadEvent(new TrainerWorkloadRequest(
                "Anna.Lee", "Anna", "Lee", true, LocalDate.now(), 60, ActionType.ADD));

        for (int i = 0; i < 10; i++) {
            notifier.onTrainingChanged(event);
        }

        verify(workloadClient, times(5)).updateWorkload(any());
        assertEquals(CircuitBreaker.State.OPEN, breaker().getState());
    }
}
