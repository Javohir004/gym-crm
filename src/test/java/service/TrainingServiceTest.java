package service;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.dao.TraineeDao;
import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.dao.TrainingDao;
import com.epam.training.gym.exception.NotFoundException;
import com.epam.training.gym.exception.ValidationException;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import com.epam.training.gym.model.TrainingType;
import com.epam.training.gym.service.TrainingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import support.LogCapture;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    private TrainingService service;
    private TrainingType yoga;
    private Trainee trainee;
    private Trainer trainer;

    @BeforeEach
    void setUp() {
        service = new TrainingService();
        service.setTrainingDao(trainingDao);
        service.setTraineeDao(traineeDao);
        service.setTrainerDao(trainerDao);

        yoga = TrainingType.builder().trainingTypeName("Yoga").build();
        trainee = Trainee.builder().build();
        trainer = Trainer.builder().specialization(yoga).build();
    }

    private void bothExist() {
        when(traineeDao.findByUsername("John.Doe")).thenReturn(Optional.of(trainee));
        when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(trainer));
    }

    // ---------- addTraining ----------

    @Test
    void addTraining_usesTrainersSpecializationAsType() {
        bothExist();
        when(trainingDao.save(any(Training.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Training result = service.addTraining("John.Doe", "Anna.Lee", "  Morning Yoga ", DATE, 60);

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
    }

    @Test
    void addTraining_logsCreatedTraining() {
        bothExist();
        when(trainingDao.save(any(Training.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (LogCapture logs = new LogCapture(TrainingService.class)) {
            service.addTraining("John.Doe", "Anna.Lee", "Morning Yoga", DATE, 60);

            assertTrue(logs.messages(Level.INFO).stream()
                    .anyMatch(m -> m.contains("Morning Yoga") && m.contains("John.Doe") && m.contains("Anna.Lee")));
        }
    }

    @Test
    void addTraining_throwsValidation_whenDurationIsNotPositive() {
        assertThrows(ValidationException.class,
                () -> service.addTraining("John.Doe", "Anna.Lee", "Morning Yoga", DATE, 0));

        verifyNoInteractions(trainingDao);
    }

    @Test
    void addTraining_throwsValidation_whenDateIsMissing() {
        assertThrows(ValidationException.class,
                () -> service.addTraining("John.Doe", "Anna.Lee", "Morning Yoga", null, 60));
    }

    @Test
    void addTraining_throwsValidation_whenNameIsBlank() {
        assertThrows(ValidationException.class,
                () -> service.addTraining("John.Doe", "Anna.Lee", " ", DATE, 60));
    }

    @Test
    void addTraining_throwsValidation_whenUsernamesAreBlank() {
        assertThrows(ValidationException.class, () -> service.addTraining(" ", "Anna.Lee", "Yoga", DATE, 60));
        assertThrows(ValidationException.class, () -> service.addTraining("John.Doe", "", "Yoga", DATE, 60));
    }

    @Test
    void addTraining_throwsNotFound_whenTraineeDoesNotExist() {
        when(traineeDao.findByUsername("John.Doe")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.addTraining("John.Doe", "Anna.Lee", "Morning Yoga", DATE, 60));

        assertTrue(ex.getMessage().contains("John.Doe"));
        verify(trainingDao, never()).save(any());
    }

    @Test
    void addTraining_throwsNotFound_whenTrainerDoesNotExist() {
        when(traineeDao.findByUsername("John.Doe")).thenReturn(Optional.of(trainee));
        when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.addTraining("John.Doe", "Anna.Lee", "Morning Yoga", DATE, 60));

        assertTrue(ex.getMessage().contains("Anna.Lee"));
        verify(trainingDao, never()).save(any());
    }

    // ---------- searching ----------

    @Test
    void getTraineeTrainings_passesAllFiltersToDao() {
        List<Training> expected = List.of(Training.builder().trainingName("Morning Yoga").build());
        LocalDate to = DATE.plusDays(30);
        when(traineeDao.findByUsername("John.Doe")).thenReturn(Optional.of(trainee));
        when(trainingDao.findByTraineeUsername("John.Doe", DATE, to, "anna", "Yoga")).thenReturn(expected);

        assertSame(expected, service.getTraineeTrainings("John.Doe", DATE, to, "anna", "Yoga"));
    }

    @Test
    void getTraineeTrainings_throwsNotFound_andDoesNotQuery_whenTraineeDoesNotExist() {
        when(traineeDao.findByUsername("John.Doe")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.getTraineeTrainings("John.Doe", null, null, null, null));

        verifyNoInteractions(trainingDao);
    }

    @Test
    void getTrainerTrainings_passesAllFiltersToDao() {
        List<Training> expected = List.of(Training.builder().trainingName("Morning Yoga").build());
        when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(trainer));
        when(trainingDao.findByTrainerUsername("Anna.Lee", DATE, null, "john")).thenReturn(expected);

        assertSame(expected, service.getTrainerTrainings("Anna.Lee", DATE, null, "john"));
    }

    @Test
    void getTrainerTrainings_throwsNotFound_andDoesNotQuery_whenTrainerDoesNotExist() {
        when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getTrainerTrainings("Anna.Lee", null, null, null));

        verifyNoInteractions(trainingDao);
    }
}