package service;

import com.epam.training.gym.dao.TraineeDao;
import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.dao.TrainingDao;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import com.epam.training.gym.model.TrainingType;
import com.epam.training.gym.service.TrainingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

    @Mock
    private TrainingDao trainingDao;

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainerDao trainerDao;

    private TrainingService trainingService;

    @BeforeEach
    void setUp() {
        trainingService = new TrainingService();
        trainingService.setTrainingDao(trainingDao);
        trainingService.setTraineeDao(traineeDao);
        trainingService.setTrainerDao(trainerDao);
    }

    private Training validTraining() {
        return Training.builder()
                .traineeId(1L)
                .trainerId(2L)
                .trainingName("Morning Yoga")
                .trainingType(new TrainingType(1L, "Yoga"))
                .trainingDate(LocalDate.now())
                .trainingDuration(60)
                .build();
    }

    @Test
    void create_savesTraining_whenTraineeAndTrainerExist() {
        when(traineeDao.select(1L)).thenReturn(Trainee.builder().userId(1L).build());
        when(trainerDao.select(2L)).thenReturn(Trainer.builder().userId(2L).build());
        when(trainingDao.selectAll()).thenReturn(List.of());
        when(trainingDao.create(any(Training.class))).thenAnswer(inv -> inv.getArgument(0));

        Training result = trainingService.create(validTraining());

        assertEquals(1L, result.getTrainingId());
        verify(trainingDao).create(any(Training.class));
    }

    @Test
    void create_assignsNextIdBasedOnExistingMaxId() {
        when(traineeDao.select(1L)).thenReturn(Trainee.builder().userId(1L).build());
        when(trainerDao.select(2L)).thenReturn(Trainer.builder().userId(2L).build());
        when(trainingDao.selectAll()).thenReturn(List.of(Training.builder().trainingId(3L).build()));
        when(trainingDao.create(any(Training.class))).thenAnswer(inv -> inv.getArgument(0));

        Training result = trainingService.create(validTraining());

        assertEquals(4L, result.getTrainingId());
    }

    @Test
    void create_throwsException_whenTraineeNotFound() {
        when(traineeDao.select(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> trainingService.create(validTraining()));
        verify(trainingDao, never()).create(any());
    }

    @Test
    void create_throwsException_whenTrainerNotFound() {
        when(traineeDao.select(1L)).thenReturn(Trainee.builder().userId(1L).build());
        when(trainerDao.select(2L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> trainingService.create(validTraining()));
        verify(trainingDao, never()).create(any());
    }

    @Test
    void create_throwsException_whenTrainingIsNull() {
        assertThrows(IllegalArgumentException.class, () -> trainingService.create(null));
    }

    @Test
    void select_returnsEmptyOptional_whenNotFound() {
        when(trainingDao.select(10L)).thenReturn(null);

        Optional<Training> result = trainingService.select(10L);

        assertFalse(result.isPresent());
    }

    @Test
    void select_returnsTraining_whenFound() {
        Training training = Training.builder().trainingId(10L).build();
        when(trainingDao.select(10L)).thenReturn(training);

        Optional<Training> result = trainingService.select(10L);

        assertTrue(result.isPresent());
    }

    @Test
    void selectAll_returnsAllTrainings() {
        when(trainingDao.selectAll()).thenReturn(List.of(
                Training.builder().trainingId(1L).build(),
                Training.builder().trainingId(2L).build()));

        List<Training> result = trainingService.selectAll();

        assertEquals(2, result.size());
    }
}
