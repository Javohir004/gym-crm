package facade;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.dto.TrainingRequest;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.facade.GymFacade;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import com.epam.training.gym.service.TraineeService;
import com.epam.training.gym.service.TrainerService;
import com.epam.training.gym.service.TrainingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import support.LogCapture;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GymFacadeTest {

    private static final String USER = "John.Doe";
    private static final String PASS = "secretPass1";

    @Mock
    private TraineeService traineeService;
    @Mock
    private TrainerService trainerService;
    @Mock
    private TrainingService trainingService;

    private GymFacade facade;

    @BeforeEach
    void setUp() {
        facade = new GymFacade(traineeService, trainerService, trainingService);
    }



    @Test
    void createTrainer_delegatesToTrainerService() {
        Trainer trainer = Trainer.builder().build();
        when(trainerService.createTrainer("Anna", "Lee", "Yoga")).thenReturn(trainer);

        assertSame(trainer, facade.createTrainer("Anna", "Lee", "Yoga"));
    }

    @Test
    void trainerCredentialsMatch_delegatesToTrainerService() {
        when(trainerService.credentialsMatch(USER, PASS)).thenReturn(true);

        assertTrue(facade.trainerCredentialsMatch(USER, PASS));
    }

    @Test
    void getTrainer_delegatesToTrainerService() {
        Trainer trainer = Trainer.builder().build();
        when(trainerService.getByUsername(USER, PASS)).thenReturn(trainer);

        assertSame(trainer, facade.getTrainer(USER, PASS));
    }

    @Test
    void changeTrainerPassword_delegatesToTrainerService() {
        facade.changeTrainerPassword(USER, PASS, "newPass");

        verify(trainerService).changePassword(USER, PASS, "newPass");
    }

    @Test
    void updateTrainer_delegatesToTrainerService() {
        Trainer trainer = Trainer.builder().build();
        when(trainerService.update(USER, PASS, "Anna", "Lee", "Fitness")).thenReturn(trainer);

        assertSame(trainer, facade.updateTrainer(USER, PASS, "Anna", "Lee", "Fitness"));
    }

    @Test
    void toggleTrainerActive_delegatesToTrainerService() {
        when(trainerService.toggleActive(USER, PASS)).thenReturn(false);

        assertFalse(facade.toggleTrainerActive(USER, PASS));
    }



    @Test
    void createTrainee_delegatesToTraineeService() {
        Trainee trainee = Trainee.builder().build();
        LocalDate birth = LocalDate.of(2000, 1, 15);
        when(traineeService.createTrainee("John", "Doe", birth, "Tashkent")).thenReturn(trainee);

        assertSame(trainee, facade.createTrainee("John", "Doe", birth, "Tashkent"));
    }

    @Test
    void traineeCredentialsMatch_delegatesToTraineeService() {
        when(traineeService.credentialsMatch(USER, PASS)).thenReturn(true);

        assertTrue(facade.traineeCredentialsMatch(USER, PASS));
    }

    @Test
    void getTrainee_delegatesToTraineeService() {
        Trainee trainee = Trainee.builder().build();
        when(traineeService.getByUsername(USER, PASS)).thenReturn(trainee);

        assertSame(trainee, facade.getTrainee(USER, PASS));
    }

    @Test
    void changeTraineePassword_delegatesToTraineeService() {
        facade.changeTraineePassword(USER, PASS, "newPass");

        verify(traineeService).changePassword(USER, PASS, "newPass");
    }

    @Test
    void updateTrainee_delegatesToTraineeService() {
        Trainee trainee = Trainee.builder().build();
        LocalDate birth = LocalDate.of(2000, 1, 15);
        when(traineeService.update(USER, PASS, "John", "Doe", birth, "Samarkand")).thenReturn(trainee);

        assertSame(trainee, facade.updateTrainee(USER, PASS, "John", "Doe", birth, "Samarkand"));
    }

    @Test
    void toggleTraineeActive_delegatesToTraineeService() {
        when(traineeService.toggleActive(USER, PASS)).thenReturn(true);

        assertTrue(facade.toggleTraineeActive(USER, PASS));
    }

    @Test
    void deleteTrainee_delegatesToTraineeService() {
        facade.deleteTrainee(USER, PASS);

        verify(traineeService).delete(USER, PASS);
    }

    @Test
    void getUnassignedTrainers_delegatesToTraineeService() {
        List<Trainer> trainers = List.of(Trainer.builder().build());
        when(traineeService.getUnassignedTrainers(USER, PASS)).thenReturn(trainers);

        assertSame(trainers, facade.getUnassignedTrainers(USER, PASS));
    }

    @Test
    void updateTraineeTrainers_delegatesToTraineeService() {
        Trainee trainee = Trainee.builder().build();
        List<String> usernames = List.of("Anna.Lee");
        when(traineeService.updateTrainers(USER, PASS, usernames)).thenReturn(trainee);

        assertSame(trainee, facade.updateTraineeTrainers(USER, PASS, usernames));
    }



    @Test
    void addTraining_delegatesToTrainingService() {
        TrainingRequest request = new TrainingRequest(USER, "Anna.Lee", "Yoga", "Yoga", LocalDate.of(2026, 9, 1), 60);
        Training training = Training.builder().build();
        when(trainingService.addTraining("Anna.Lee", PASS, request)).thenReturn(training);

        assertSame(training, facade.addTraining("Anna.Lee", PASS, request));
    }

    @Test
    void getTraineeTrainings_delegatesToTrainingService() {
        List<Training> trainings = List.of(Training.builder().build());
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        when(trainingService.getTraineeTrainings(USER, PASS, from, to, "anna", "Yoga")).thenReturn(trainings);

        assertSame(trainings, facade.getTraineeTrainings(USER, PASS, from, to, "anna", "Yoga"));
    }

    @Test
    void getTrainerTrainings_delegatesToTrainingService() {
        List<Training> trainings = List.of(Training.builder().build());
        when(trainingService.getTrainerTrainings("Anna.Lee", PASS, null, null, "john")).thenReturn(trainings);

        assertSame(trainings, facade.getTrainerTrainings("Anna.Lee", PASS, null, null, "john"));
    }


    @Test
    void serviceExceptions_areNotSwallowedByFacade() {
        when(traineeService.getByUsername(USER, "wrong"))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        assertThrows(AuthenticationException.class, () -> facade.getTrainee(USER, "wrong"));
    }


    @Test
    void operations_areLoggedWithUsername_butNeverWithPasswords() {
        try (LogCapture logs = new LogCapture(GymFacade.class)) {
            facade.getTrainee(USER, PASS);
            facade.changeTraineePassword(USER, PASS, "brandNewSecret");
            facade.traineeCredentialsMatch(USER, PASS);

            assertTrue(logs.messages(Level.INFO).size() >= 3);
            assertTrue(logs.messages(Level.INFO).stream().allMatch(m -> m.contains("Facade:")));
            assertTrue(logs.messages(Level.INFO).stream().anyMatch(m -> m.contains(USER)));
            assertTrue(logs.allMessages().stream().noneMatch(m -> m.contains(PASS)));
            assertTrue(logs.allMessages().stream().noneMatch(m -> m.contains("brandNewSecret")));
        }
    }
}