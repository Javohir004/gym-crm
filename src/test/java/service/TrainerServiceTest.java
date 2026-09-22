package service;


import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.TrainingType;
import com.epam.training.gym.service.PasswordGenerator;
import com.epam.training.gym.service.TrainerService;
import com.epam.training.gym.service.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class TrainerServiceTest {

    @Mock
    private TrainerDao trainerDao;

    @Mock
    private UsernameGenerator usernameGenerator;

    @Mock
    private PasswordGenerator passwordGenerator;

    private TrainerService trainerService;

    @BeforeEach
    void setUp() {
        trainerService = new TrainerService();
        trainerService.setTrainerDao(trainerDao);
        trainerService.setUsernameGenerator(usernameGenerator);
        trainerService.setPasswordGenerator(passwordGenerator);
    }

    @Test
    void create_generatesIdUsernamePasswordAndSavesTrainer() {
        Trainer input = Trainer.builder()
                .firstName("Anna")
                .lastName("Lee")
                .specialization(new TrainingType(1L, "Yoga"))
                .build();

        when(trainerDao.selectAll()).thenReturn(List.of());
        when(usernameGenerator.generate("Anna", "Lee")).thenReturn("Anna.Lee");
        when(passwordGenerator.generate()).thenReturn("qWe4rTy8ui");
        when(trainerDao.create(any(Trainer.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainer result = trainerService.create(input);

        assertEquals(1L, result.getUserId());
        assertEquals("Anna.Lee", result.getUsername());
        assertEquals("qWe4rTy8ui", result.getPassword());
        assertTrue(result.isActive());
    }

    @Test
    void create_throwsException_whenTrainerIsNull() {
        assertThrows(IllegalArgumentException.class, () -> trainerService.create(null));
    }

    @Test
    void update_updatesExistingTrainer() {
        Trainer trainer = Trainer.builder().userId(1L).specialization(new TrainingType(2L, "Fitness")).build();
        when(trainerDao.select(1L)).thenReturn(Trainer.builder().userId(1L).build());
        when(trainerDao.update(trainer)).thenReturn(trainer);

        Trainer result = trainerService.update(trainer);

        assertEquals("Fitness", result.getSpecialization().getTrainingTypeName());
        verify(trainerDao).update(trainer);
    }

    @Test
    void update_throwsException_whenTrainerDoesNotExist() {
        Trainer trainer = Trainer.builder().userId(99L).build();
        when(trainerDao.select(99L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> trainerService.update(trainer));
        verify(trainerDao, never()).update(any());
    }

    @Test
    void select_returnsEmptyOptional_whenNotFound() {
        when(trainerDao.select(5L)).thenReturn(null);

        Optional<Trainer> result = trainerService.select(5L);

        assertFalse(result.isPresent());
    }

    @Test
    void select_returnsTrainer_whenFound() {
        Trainer trainer = Trainer.builder().userId(5L).build();
        when(trainerDao.select(5L)).thenReturn(trainer);

        Optional<Trainer> result = trainerService.select(5L);

        assertTrue(result.isPresent());
    }

    @Test
    void selectAll_returnsAllTrainers() {
        when(trainerDao.selectAll()).thenReturn(List.of(
                Trainer.builder().userId(1L).build(),
                Trainer.builder().userId(2L).build()));

        List<Trainer> result = trainerService.selectAll();

        assertEquals(2, result.size());
    }
}
