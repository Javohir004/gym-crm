package facade;

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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class GymFacadeTest {

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
    void createTrainee_delegatesToTraineeService() {
        Trainee trainee = Trainee.builder().firstName("John").lastName("Doe").build();
        when(traineeService.create(trainee)).thenReturn(trainee);

        Trainee result = facade.createTrainee(trainee);

        assertEquals(trainee, result);
        verify(traineeService).create(trainee);
    }

    @Test
    void updateTrainee_delegatesToTraineeService() {
        Trainee trainee = Trainee.builder().userId(1L).build();
        when(traineeService.update(trainee)).thenReturn(trainee);

        facade.updateTrainee(trainee);

        verify(traineeService).update(trainee);
    }

    @Test
    void deleteTrainee_delegatesToTraineeService() {
        facade.deleteTrainee(1L);

        verify(traineeService).delete(1L);
    }

    @Test
    void selectTrainee_delegatesToTraineeService() {
        when(traineeService.select(1L)).thenReturn(Optional.of(Trainee.builder().userId(1L).build()));

        Optional<Trainee> result = facade.selectTrainee(1L);

        assertEquals(1L, result.get().getUserId());
    }

    @Test
    void selectAllTrainees_delegatesToTraineeService() {
        when(traineeService.selectAll()).thenReturn(List.of(Trainee.builder().userId(1L).build()));

        List<Trainee> result = facade.selectAllTrainees();

        assertEquals(1, result.size());
    }

    @Test
    void createTrainer_delegatesToTrainerService() {
        Trainer trainer = Trainer.builder().firstName("Anna").lastName("Lee").build();
        when(trainerService.create(trainer)).thenReturn(trainer);

        Trainer result = facade.createTrainer(trainer);

        assertEquals(trainer, result);
        verify(trainerService).create(trainer);
    }

    @Test
    void updateTrainer_delegatesToTrainerService() {
        Trainer trainer = Trainer.builder().userId(1L).build();
        when(trainerService.update(trainer)).thenReturn(trainer);

        facade.updateTrainer(trainer);

        verify(trainerService).update(trainer);
    }

    @Test
    void selectTrainer_delegatesToTrainerService() {
        when(trainerService.select(1L)).thenReturn(Optional.empty());

        Optional<Trainer> result = facade.selectTrainer(1L);

        assertFalse(result.isPresent());
    }

    @Test
    void selectAllTrainers_delegatesToTrainerService() {
        when(trainerService.selectAll()).thenReturn(List.of(Trainer.builder().userId(1L).build()));

        List<Trainer> result = facade.selectAllTrainers();

        assertEquals(1, result.size());
    }

    @Test
    void createTraining_delegatesToTrainingService() {
        Training training = Training.builder().trainingName("Yoga").build();
        when(trainingService.create(training)).thenReturn(training);

        Training result = facade.createTraining(training);

        assertEquals(training, result);
        verify(trainingService).create(training);
    }

    @Test
    void selectTraining_delegatesToTrainingService() {
        when(trainingService.select(5L)).thenReturn(Optional.of(Training.builder().trainingId(5L).build()));

        Optional<Training> result = facade.selectTraining(5L);

        assertEquals(5L, result.get().getTrainingId());
    }

    @Test
    void selectAllTrainings_delegatesToTrainingService() {
        when(trainingService.selectAll()).thenReturn(List.of(Training.builder().trainingId(1L).build()));

        List<Training> result = facade.selectAllTrainings();

        assertEquals(1, result.size());
    }
}