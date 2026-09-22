package service;

import com.epam.training.gym.dao.TraineeDao;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.service.PasswordGenerator;
import com.epam.training.gym.service.TraineeService;
import com.epam.training.gym.service.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraineeServiceTest {

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private UsernameGenerator usernameGenerator;

    @Mock
    private PasswordGenerator passwordGenerator;

    private TraineeService traineeService;

    @BeforeEach
    void setUp() {
        traineeService = new TraineeService();
        traineeService.setTraineeDao(traineeDao);
        traineeService.setUsernameGenerator(usernameGenerator);
        traineeService.setPasswordGenerator(passwordGenerator);
    }

    @Test
    void create_generatesIdUsernamePasswordAndSavesTrainee() {
        Trainee input = Trainee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .address("Tashkent")
                .build();

        when(traineeDao.selectAll()).thenReturn(List.of());
        when(usernameGenerator.generate("John", "Doe")).thenReturn("John.Doe");
        when(passwordGenerator.generate()).thenReturn("aB3dE7fG9h");
        when(traineeDao.create(any(Trainee.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainee result = traineeService.create(input);

        assertEquals(1L, result.getUserId());
        assertEquals("John.Doe", result.getUsername());
        assertEquals("aB3dE7fG9h", result.getPassword());
        assertTrue(result.isActive());

        ArgumentCaptor<Trainee> captor = ArgumentCaptor.forClass(Trainee.class);
        verify(traineeDao).create(captor.capture());
        assertEquals("John.Doe", captor.getValue().getUsername());
    }

    @Test
    void create_assignsNextIdBasedOnExistingMaxId() {
        Trainee existing = Trainee.builder().userId(5L).build();
        when(traineeDao.selectAll()).thenReturn(List.of(existing));
        when(usernameGenerator.generate(any(), any())).thenReturn("Jane.Roe");
        when(passwordGenerator.generate()).thenReturn("pass12345x");
        when(traineeDao.create(any(Trainee.class))).thenAnswer(inv -> inv.getArgument(0));

        Trainee result = traineeService.create(
                Trainee.builder().firstName("Jane").lastName("Roe").build());

        assertEquals(6L, result.getUserId());
    }

    @Test
    void create_throwsException_whenTraineeIsNull() {
        assertThrows(IllegalArgumentException.class, () -> traineeService.create(null));
    }

    @Test
    void update_updatesExistingTrainee() {
        Trainee trainee = Trainee.builder().userId(1L).address("New address").build();
        when(traineeDao.select(1L)).thenReturn(Trainee.builder().userId(1L).build());
        when(traineeDao.update(trainee)).thenReturn(trainee);

        Trainee result = traineeService.update(trainee);

        assertEquals("New address", result.getAddress());
        verify(traineeDao).update(trainee);
    }

    @Test
    void update_throwsException_whenTraineeDoesNotExist() {
        Trainee trainee = Trainee.builder().userId(99L).build();
        when(traineeDao.select(99L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> traineeService.update(trainee));
        verify(traineeDao, never()).update(any());
    }

    @Test
    void update_throwsException_whenIdIsNull() {
        Trainee trainee = Trainee.builder().build();

        assertThrows(IllegalArgumentException.class, () -> traineeService.update(trainee));
    }

    @Test
    void delete_removesExistingTrainee() {
        when(traineeDao.select(1L)).thenReturn(Trainee.builder().userId(1L).build());

        traineeService.delete(1L);

        verify(traineeDao, times(1)).delete(1L);
    }

    @Test
    void delete_doesNothing_whenTraineeDoesNotExist() {
        when(traineeDao.select(42L)).thenReturn(null);

        traineeService.delete(42L);

        verify(traineeDao, never()).delete(any());
    }

    @Test
    void delete_throwsException_whenIdIsNull() {
        assertThrows(IllegalArgumentException.class, () -> traineeService.delete(null));
    }

    @Test
    void select_returnsEmptyOptional_whenNotFound() {
        when(traineeDao.select(7L)).thenReturn(null);

        Optional<Trainee> result = traineeService.select(7L);

        assertFalse(result.isPresent());
    }

    @Test
    void select_returnsTrainee_whenFound() {
        Trainee trainee = Trainee.builder().userId(7L).build();
        when(traineeDao.select(7L)).thenReturn(trainee);

        Optional<Trainee> result = traineeService.select(7L);

        assertTrue(result.isPresent());
        assertEquals(7L, result.get().getUserId());
    }

    @Test
    void selectAll_returnsAllTrainees() {
        when(traineeDao.selectAll()).thenReturn(List.of(
                Trainee.builder().userId(1L).build(),
                Trainee.builder().userId(2L).build()));

        List<Trainee> result = traineeService.selectAll();

        assertEquals(2, result.size());
    }
}
