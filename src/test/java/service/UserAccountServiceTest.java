package service;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.dao.UserDao;
import com.epam.training.gym.exception.ValidationException;
import com.epam.training.gym.model.User;
import com.epam.training.gym.service.PasswordGenerator;
import com.epam.training.gym.service.UserAccountService;
import com.epam.training.gym.service.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import support.LogCapture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserDao userDao;
    @Mock
    private UsernameGenerator usernameGenerator;
    @Mock
    private PasswordGenerator passwordGenerator;

    private UserAccountService service;

    @BeforeEach
    void setUp() {
        service = new UserAccountService();
        service.setUserDao(userDao);
        service.setUsernameGenerator(usernameGenerator);
        service.setPasswordGenerator(passwordGenerator);
    }

    @Test
    void newUser_buildsActiveUserWithGeneratedCredentials() {
        when(usernameGenerator.generate("John", "Doe")).thenReturn("John.Doe");
        when(passwordGenerator.generate()).thenReturn("aB3dE7fG9h");

        User user = service.newUser("John", "Doe");

        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("John.Doe", user.getUsername());
        assertEquals("aB3dE7fG9h", user.getPassword());
        assertTrue(user.isActive());
    }

    @Test
    void newUser_trimsNames() {
        when(usernameGenerator.generate(" Anna ", " Lee ")).thenReturn("Anna.Lee");
        when(passwordGenerator.generate()).thenReturn("pass123456");

        User user = service.newUser(" Anna ", " Lee ");

        assertEquals("Anna", user.getFirstName());
        assertEquals("Lee", user.getLastName());
    }

    @Test
    void newUser_throws_whenFirstNameIsBlank() {
        assertThrows(ValidationException.class, () -> service.newUser(" ", "Doe"));
        verifyNoInteractions(usernameGenerator, passwordGenerator);
    }

    @Test
    void newUser_throws_whenLastNameIsNull() {
        assertThrows(ValidationException.class, () -> service.newUser("John", null));
        verifyNoInteractions(usernameGenerator, passwordGenerator);
    }

    @Test
    void changePassword_updatesUserAndPersistsIt() {
        User user = User.builder().username("John.Doe").password("old").build();

        service.changePassword(user, "brandNewPass");

        assertEquals("brandNewPass", user.getPassword());
        verify(userDao).update(user);
    }

    @Test
    void changePassword_throws_whenNewPasswordIsBlank() {
        User user = User.builder().username("John.Doe").password("old").build();

        assertThrows(ValidationException.class, () -> service.changePassword(user, "  "));

        assertEquals("old", user.getPassword());
        verify(userDao, never()).update(user);
    }

    @Test
    void changePassword_isLogged_withoutLeakingThePassword() {
        User user = User.builder().username("John.Doe").password("old").build();

        try (LogCapture logs = new LogCapture(UserAccountService.class)) {
            service.changePassword(user, "brandNewPass");

            assertTrue(logs.messages(Level.INFO).stream().anyMatch(m -> m.contains("John.Doe")));
            assertTrue(logs.allMessages().stream().noneMatch(m -> m.contains("brandNewPass")));
            assertTrue(logs.allMessages().stream().noneMatch(m -> m.contains("old")));
        }
    }

    @Test
    void toggleActive_deactivatesActiveUser() {
        User user = User.builder().username("John.Doe").active(true).build();

        boolean result = service.toggleActive(user);

        assertFalse(result);
        assertFalse(user.isActive());
        verify(userDao).update(user);
    }

    @Test
    void toggleActive_activatesInactiveUser() {
        User user = User.builder().username("John.Doe").active(false).build();

        boolean result = service.toggleActive(user);

        assertTrue(result);
        assertTrue(user.isActive());
        verify(userDao).update(user);
    }

    @Test
    void toggleActive_logsNewState() {
        User user = User.builder().username("John.Doe").active(true).build();

        try (LogCapture logs = new LogCapture(UserAccountService.class)) {
            service.toggleActive(user);

            assertTrue(logs.messages(Level.INFO).stream()
                    .anyMatch(m -> m.contains("John.Doe") && m.contains("inactive")));
        }
    }
}