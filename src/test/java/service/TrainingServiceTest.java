package service;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.dao.TraineeDao;
import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.dao.TrainingDao;
import com.epam.training.gym.dao.TrainingTypeDao;
import com.epam.training.gym.dto.TrainingRequest;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.exception.NotFoundException;
import com.epam.training.gym.exception.ValidationException;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import com.epam.training.gym.model.TrainingType;
import com.epam.training.gym.service.AuthenticationService;
import com.epam.training.gym.service.TrainingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import support.LogCapture;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 1);

    @Mock
    private TrainingDao trainingDao;
    @Mock
    private TraineeDao traineeDao;
    @Mock
    private TrainerDao trainerDao;
    @Mock
    private TrainingTypeDao trainingTypeDao;
    @Mock
    private AuthenticationService authenticationService;

    private TrainingService service;

    @BeforeEach
    void setUp() {
        service = new TrainingService();
        service.setTrainingDao(trainingDao);
        service.setTraineeDao(traineeDao);
        service.setTrainerDao(trainerDao);
        service.setTrainingTypeDao(trainingTypeDao);
        service.setAuthenticationService(authenticationService);
    }

    private static TrainingRequest validRequest() {
        return new TrainingRequest("John.Doe", "Anna.Lee", "Morning Yoga", "Yoga", DATE, 60);
    }


    @Test
    void addTraining_savesTrainingBuiltFromRequest() {
        Trainee trainee = Trainee.builder().build();
        Trainer trainer = Trainer.builder().build();
        TrainingType yoga = TrainingType.builder().trainingTypeName("Yoga").build();
        when(traineeDao.findByUsername("John.Doe")).thenReturn(Optional.of(trainee));
        when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(trainer));
        when(trainingTypeDao.findByName("Yoga")).thenReturn(Optional.of(yoga));
        when(trainingDao.save(any(Training.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Training result = service.addTraining("Anna.Lee", "pass", validRequest());

        ArgumentCaptor<Training> captor = ArgumentCaptor.forClass(Training.class);
        verify(trainingDao).save(captor.capture());
        Training saved = captor.getValue();
        assertSame(trainee, saved.getTrainee());
        assertSame(trainer, saved.getTrainer());
        assertSame(yoga, saved.getTrainingType());
        assertEquals("Morning Yoga", saved.getTrainingName());
        assertEquals(DATE, saved.getTrainingDate());
        assertEquals(60, saved.getTrainingDuration());
        assertSame(saved, result);
        verify(authenticationService).authenticate("Anna.Lee", "pass");
    }

    @Test
    void addTraining_trimsTrainingNameAndTypeName() {
        TrainingType yoga = TrainingType.builder().trainingTypeName("Yoga").build();
        when(traineeDao.findByUsername("John.Doe")).thenReturn(Optional.of(Trainee.builder().build()));
        when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(Trainer.builder().build()));
        when(trainingTypeDao.findByName("Yoga")).thenReturn(Optional.of(yoga));
        when(trainingDao.save(any(Training.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Training result = service.addTraining("Anna.Lee", "pass",
                new TrainingRequest("John.Doe", "Anna.Lee", "  Morning Yoga  ", " Yoga ", DATE, 60));

        assertEquals("Morning Yoga", result.getTrainingName());
    }

    @Test
    void addTraining_logsCreatedTraining() {
        TrainingType yoga = TrainingType.builder().trainingTypeName("Yoga").build();
        when(traineeDao.findByUsername("John.Doe")).thenReturn(Optional.of(Trainee.builder().build()));
        when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(Trainer.builder().build()));
        when(trainingTypeDao.findByName("Yoga")).thenReturn(Optional.of(yoga));
        when(trainingDao.save(any(Training.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (LogCapture logs = new LogCapture(TrainingService.class)) {
            service.addTraining("Anna.Lee", "pass", validRequest());

            assertTrue(logs.messages(Level.INFO).stream()
                    .anyMatch(m -> m.contains("Morning Yoga") && m.contains("John.Doe") && m.contains("Anna.Lee")));
        }
    }

    @Test
    void addTraining_throwsAndSavesNothing_whenAuthenticationFails() {
        when(authenticationService.authenticate("Anna.Lee", "wrong"))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        assertThrows(AuthenticationException.class, () -> service.addTraining("Anna.Lee", "wrong", validRequest()));

        verifyNoInteractions(trainingDao, traineeDao, trainerDao, trainingTypeDao);
    }

    @Test
    void addTraining_throwsValidation_whenRequestIsNull() {
        assertThrows(ValidationException.class, () -> service.addTraining("Anna.Lee", "pass", null));

        verifyNoInteractions(trainingDao);
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of(new TrainingRequest(" ", "Anna.Lee", "Yoga", "Yoga", DATE, 60), "Trainee username"),
                Arguments.of(new TrainingRequest("John.Doe", null, "Yoga", "Yoga", DATE, 60), "Trainer username"),
                Arguments.of(new TrainingRequest("John.Doe", "Anna.Lee", "", "Yoga", DATE, 60), "Training name"),
                Arguments.of(new TrainingRequest("John.Doe", "Anna.Lee", "Yoga", null, DATE, 60), "Training type"),
                Arguments.of(new TrainingRequest("John.Doe", "Anna.Lee", "Yoga", "Yoga", null, 60), "Training date"),
                Arguments.of(new TrainingRequest("John.Doe", "Anna.Lee", "Yoga", "Yoga", DATE, null), "Training duration"),
                Arguments.of(new TrainingRequest("John.Doe", "Anna.Lee", "Yoga", "Yoga", DATE, 0), "Training duration"),
                Arguments.of(new TrainingRequest("John.Doe", "Anna.Lee", "Yoga", "Yoga", DATE, -5), "Training duration")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void addTraining_throwsValidation_forInvalidField(TrainingRequest request, String fieldInMessage) {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.addTraining("Anna.Lee", "pass", request));

        assertTrue(ex.getMessage().contains(fieldInMessage));
        verify(trainingDao, never()).save(any());
    }

    @Test
    void addTraining_throwsNotFound_whenTraineeDoesNotExist() {
        when(traineeDao.findByUsername("John.Doe")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.addTraining("Anna.Lee", "pass", validRequest()));

        assertTrue(ex.getMessage().contains("John.Doe"));
        verify(trainingDao, never()).save(any());
    }

    @Test
    void addTraining_throwsNotFound_whenTrainerDoesNotExist() {
        when(traineeDao.findByUsername("John.Doe")).thenReturn(Optional.of(Trainee.builder().build()));
        when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.addTraining("Anna.Lee", "pass", validRequest()));

        assertTrue(ex.getMessage().contains("Anna.Lee"));
        verify(trainingDao, never()).save(any());
    }

    @Test
    void addTraining_throwsNotFound_whenTrainingTypeDoesNotExist() {
        when(traineeDao.findByUsername("John.Doe")).thenReturn(Optional.of(Trainee.builder().build()));
        when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(Trainer.builder().build()));
        when(trainingTypeDao.findByName("Yoga")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.addTraining("Anna.Lee", "pass", validRequest()));

        assertTrue(ex.getMessage().contains("Yoga"));
        verify(trainingDao, never()).save(any());
    }


    @Test
    void getTraineeTrainings_authenticatesAsTrainee_andPassesAllFiltersToDao() {
        List<Training> expected = List.of(Training.builder().trainingName("Morning Yoga").build());
        LocalDate to = DATE.plusDays(30);
        when(trainingDao.findByTraineeUsername("John.Doe", DATE, to, "anna", "Yoga")).thenReturn(expected);

        List<Training> result = service.getTraineeTrainings("John.Doe", "pass", DATE, to, "anna", "Yoga");

        assertSame(expected, result);
        verify(authenticationService).authenticateTrainee("John.Doe", "pass");
    }

    @Test
    void getTraineeTrainings_doesNotQuery_whenAuthenticationFails() {
        when(authenticationService.authenticateTrainee("John.Doe", "wrong"))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        assertThrows(AuthenticationException.class,
                () -> service.getTraineeTrainings("John.Doe", "wrong", null, null, null, null));

        verifyNoInteractions(trainingDao);
    }

    @Test
    void getTrainerTrainings_authenticatesAsTrainer_andPassesAllFiltersToDao() {
        List<Training> expected = List.of(Training.builder().trainingName("Morning Yoga").build());
        when(trainingDao.findByTrainerUsername("Anna.Lee", DATE, null, "john")).thenReturn(expected);

        List<Training> result = service.getTrainerTrainings("Anna.Lee", "pass", DATE, null, "john");

        assertSame(expected, result);
        verify(authenticationService).authenticateTrainer("Anna.Lee", "pass");
    }

    @Test
    void getTrainerTrainings_doesNotQuery_whenAuthenticationFails() {
        when(authenticationService.authenticateTrainer("Anna.Lee", "wrong"))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        assertThrows(AuthenticationException.class,
                () -> service.getTrainerTrainings("Anna.Lee", "wrong", null, null, null));

        verifyNoInteractions(trainingDao);
    }
}