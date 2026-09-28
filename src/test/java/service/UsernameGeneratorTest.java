package service;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.dao.UserDao;
import com.epam.training.gym.exception.ValidationException;
import com.epam.training.gym.service.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import support.LogCapture;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsernameGeneratorTest {

    @Mock
    private UserDao userDao;

    private UsernameGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new UsernameGenerator();
        generator.setUserDao(userDao);
    }

    @Test
    void generate_returnsBaseUsername_whenNotTaken() {
        when(userDao.findUsernamesStartingWith("John.Doe")).thenReturn(List.of());

        assertEquals("John.Doe", generator.generate("John", "Doe"));
    }

    @Test
    void generate_addsSerialOne_whenBaseIsTaken() {
        when(userDao.findUsernamesStartingWith("John.Doe")).thenReturn(List.of("John.Doe"));

        assertEquals("John.Doe1", generator.generate("John", "Doe"));
    }

    @Test
    void generate_incrementsSerial_whenPreviousSerialsAreTaken() {
        when(userDao.findUsernamesStartingWith("John.Doe"))
                .thenReturn(List.of("John.Doe", "John.Doe1", "John.Doe2"));

        assertEquals("John.Doe3", generator.generate("John", "Doe"));
    }

    @Test
    void generate_usesFirstFreeSerial_whenThereIsAGap() {
        when(userDao.findUsernamesStartingWith("John.Doe"))
                .thenReturn(List.of("John.Doe", "John.Doe2"));

        assertEquals("John.Doe1", generator.generate("John", "Doe"));
    }

    @Test
    void generate_ignoresLongerNamesThatOnlySharePrefix() {
        when(userDao.findUsernamesStartingWith("John.Doe")).thenReturn(List.of("John.Doey"));

        assertEquals("John.Doe", generator.generate("John", "Doe"));
    }

    @Test
    void generate_trimsNames() {
        when(userDao.findUsernamesStartingWith("John.Doe")).thenReturn(List.of());

        assertEquals("John.Doe", generator.generate("  John ", " Doe  "));
    }

    @Test
    void generate_throws_whenFirstNameIsBlank() {
        assertThrows(ValidationException.class, () -> generator.generate(" ", "Doe"));
        verifyNoInteractions(userDao);
    }

    @Test
    void generate_throws_whenLastNameIsNull() {
        assertThrows(ValidationException.class, () -> generator.generate("John", null));
        verifyNoInteractions(userDao);
    }

    @Test
    void generate_logsGeneratedUsername() {
        when(userDao.findUsernamesStartingWith("John.Doe")).thenReturn(List.of("John.Doe"));

        try (LogCapture logs = new LogCapture(UsernameGenerator.class)) {
            generator.generate("John", "Doe");

            assertTrue(logs.messages(Level.DEBUG).stream().anyMatch(m -> m.contains("John.Doe1")));
        }
    }
}