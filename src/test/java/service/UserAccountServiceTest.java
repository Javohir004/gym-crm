package service;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.dao.UserDao;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.exception.ConflictException;
import com.epam.training.gym.exception.ValidationException;
import com.epam.training.gym.model.User;
import com.epam.training.gym.service.AuthenticationService;
import com.epam.training.gym.service.PasswordGenerator;
import com.epam.training.gym.service.UserAccountService;
import com.epam.training.gym.service.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import support.LogCapture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    @Mock
    private AuthenticationService authenticationService;

    // low cost factor keeps the tests fast; production uses the BCrypt default
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private UserAccountService service;

    @BeforeEach
    void setUp() {
        service = new UserAccountService();
        service.setUserDao(userDao);
        service.setUsernameGenerator(usernameGenerator);
        service.setPasswordGenerator(passwordGenerator);
        service.setPasswordEncoder(passwordEncoder);
        service.setAuthenticationService(authenticationService);
    }

    @Test
    void newUser_buildsActiveUserWithGeneratedCredentials() {
        when(usernameGenerator.generate("John", "Doe")).thenReturn("John.Doe");
        when(passwordGenerator.generate()).thenReturn("aB3dE7fG9h");

        UserAccountService.NewAccount account = service.newUser("John", "Doe");
        User user = account.user();

        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("John.Doe", user.getUsername());
        assertEquals("aB3dE7fG9h", account.rawPassword());
        assertTrue(user.isActive());
    }

    @Test
    void newUser_storesOnlyTheBcryptHash_neverThePlainPassword() {
        when(usernameGenerator.generate("John", "Doe")).thenReturn("John.Doe");
        when(passwordGenerator.generate()).thenReturn("aB3dE7fG9h");

        UserAccountService.NewAccount account = service.newUser("John", "Doe");

        assertNotEquals("aB3dE7fG9h", account.user().getPassword());
        assertTrue(account.user().getPassword().startsWith("$2a$"));
        assertTrue(passwordEncoder.matches("aB3dE7fG9h", account.user().getPassword()));
        assertFalse(account.toString().contains("aB3dE7fG9h"));
    }

    @Test
    void newUser_trimsNames() {
        when(usernameGenerator.generate(" Anna ", " Lee ")).thenReturn("Anna.Lee");
        when(passwordGenerator.generate()).thenReturn("pass123456");

        User user = service.newUser(" Anna ", " Lee ").user();

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

        assertTrue(passwordEncoder.matches("brandNewPass", user.getPassword()));
        assertNotEquals("brandNewPass", user.getPassword());
        verify(userDao).update(user);
    }

    @Test
    void changePasswordWithOldPassword_authenticatesThenStoresNewHash() {
        User user = User.builder().username("John.Doe").password("old-hash").build();
        when(authenticationService.authenticate("John.Doe", "oldPass")).thenReturn(user);

        service.changePassword("John.Doe", "oldPass", "brandNewPass");

        assertTrue(passwordEncoder.matches("brandNewPass", user.getPassword()));
        verify(userDao).update(user);
    }

    @Test
    void changePasswordWithWrongOldPassword_throwsAndDoesNotUpdate() {
        AuthenticationException failure = new AuthenticationException("Invalid username or password");
        when(authenticationService.authenticate("John.Doe", "wrong")).thenThrow(failure);

        AuthenticationException thrown = assertThrows(AuthenticationException.class,
                () -> service.changePassword("John.Doe", "wrong", "brandNewPass"));

        assertSame(failure, thrown);
        verifyNoInteractions(userDao);
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


    // ---------- REST-only: setActive(User, boolean) - explicit value, not idempotent ----------

    @Test
    void setActive_changesState_whenDifferentFromCurrent() {
        User user = User.builder().username("John.Doe").active(true).build();

        service.setActive(user, false);

        assertFalse(user.isActive());
        verify(userDao).update(user);
    }

    @Test
    void setActive_activatesInactiveUser() {
        User user = User.builder().username("John.Doe").active(false).build();

        service.setActive(user, true);

        assertTrue(user.isActive());
        verify(userDao).update(user);
    }

    @Test
    void setActive_throwsConflict_whenAlreadyInRequestedState() {
        User user = User.builder().username("John.Doe").active(true).build();

        ConflictException ex = assertThrows(ConflictException.class, () -> service.setActive(user, true));

        assertTrue(ex.getMessage().contains("John.Doe"));
        assertTrue(user.isActive());
        verify(userDao, never()).update(any());
    }

    @Test
    void setActive_throwsConflict_whenAlreadyInactiveAndAskedToDeactivate() {
        User user = User.builder().username("John.Doe").active(false).build();

        assertThrows(ConflictException.class, () -> service.setActive(user, false));

        verify(userDao, never()).update(any());
    }
}