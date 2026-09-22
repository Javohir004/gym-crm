package service;

import com.epam.training.gym.dao.TraineeDao;
import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.service.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsernameGeneratorTest {

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainerDao trainerDao;

    private UsernameGenerator usernameGenerator;

    @BeforeEach
    void setUp() {
        usernameGenerator = new UsernameGenerator();
        usernameGenerator.setTraineeDao(traineeDao);
        usernameGenerator.setTrainerDao(trainerDao);
    }

    @Test
    void generate_returnsBaseUsername_whenNotTaken() {
        when(traineeDao.getAllUsernames()).thenReturn(Set.of());
        when(trainerDao.getAllUsernames()).thenReturn(Set.of());

        String username = usernameGenerator.generate("John", "Smith");

        assertEquals("John.Smith", username);
    }

    @Test
    void generate_appendsSerial_whenBaseUsernameTaken() {
        when(traineeDao.getAllUsernames()).thenReturn(Set.of("John.Smith"));
        when(trainerDao.getAllUsernames()).thenReturn(Set.of());

        String username = usernameGenerator.generate("John", "Smith");

        assertEquals("John.Smith1", username);
    }

    @Test
    void generate_incrementsSerial_untilFreeSlotFound() {
        when(traineeDao.getAllUsernames()).thenReturn(Set.of("John.Smith", "John.Smith1", "John.Smith2"));
        when(trainerDao.getAllUsernames()).thenReturn(Set.of());

        String username = usernameGenerator.generate("John", "Smith");

        assertEquals("John.Smith3", username);
    }

    @Test
    void generate_checksUniquenessAcrossTraineesAndTrainers() {
        when(traineeDao.getAllUsernames()).thenReturn(Set.of());
        when(trainerDao.getAllUsernames()).thenReturn(Set.of("John.Smith"));

        String username = usernameGenerator.generate("John", "Smith");

        assertEquals("John.Smith1", username);
    }

    @Test
    void generate_throwsException_whenFirstNameBlank() {
        assertThrows(IllegalArgumentException.class, () -> usernameGenerator.generate(" ", "Smith"));
    }

    @Test
    void generate_throwsException_whenLastNameNull() {
        assertThrows(IllegalArgumentException.class, () -> usernameGenerator.generate("John", null));
    }
}