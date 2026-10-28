package service;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.dao.UserDao;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.exception.ConflictException;
import com.epam.training.gym.exception.ValidationException;
import com.epam.training.gym.model.User;
import com.epam.training.gym.service.PasswordGenerator;
import com.epam.training.gym.service.Registration;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserAccountService service;

    @BeforeEach
    void setUp() {
        service = new UserAccountService();
        service.setUserDao(userDao);
        service.setUsernameGenerator(usernameGenerator);
        service.setPasswordGenerator(passwordGenerator);
        service.setPasswordEncoder(passwordEncoder);
    }

    private User userWithPassword(String rawPassword) {
        return User.builder().username("John.Doe").password(passwordEncoder.encode(rawPassword)).active(true).build();
    }

    // ---------- newUser ----------

    @Test
    void newUser_buildsActiveUser_andStoresOnlyTheHashOfTheGeneratedPassword() {
        when(usernameGenerator.generate("John", "Doe")).thenReturn("John.Doe");
        when(passwordGenerator.generate()).thenReturn("aB3dE7fG9h");

        Registration<User> registration = service.newUser("John", "Doe");

        User user = registration.profile();
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("John.Doe", user.getUsername());
        assertTrue(user.isActive());
        assertEquals("aB3dE7fG9h", registration.rawPassword());
        assertNotEquals("aB3dE7fG9h", user.getPassword());
        assertTrue(passwordEncoder.matches("aB3dE7fG9h", user.getPassword()));
    }

    @Test
    void newUser_usesDifferentSalt_forTheSamePassword() {
        when(usernameGenerator.generate("John", "Doe")).thenReturn("John.Doe");
        when(passwordGenerator.generate()).thenReturn("samePassword");

        String first = service.newUser("John", "Doe").profile().getPassword();
        String second = service.newUser("John", "Doe").profile().getPassword();

        assertNotEquals(first, second);
    }

    @Test
    void newUser_trimsNames() {
        when(usernameGenerator.generate(" Anna ", " Lee ")).thenReturn("Anna.Lee");
        when(passwordGenerator.generate()).thenReturn("pass123456");

        User user = service.newUser(" Anna ", " Lee ").profile();

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

    // ---------- changePassword ----------

    @Test
    void changePassword_storesHashOfNewPassword_andPersistsUser() {
        User user = userWithPassword("oldPass123");
        when(userDao.findByUsername("John.Doe")).thenReturn(Optional.of(user));

        service.changePassword("John.Doe", "oldPass123", "brandNewPass");

        assertNotEquals("brandNewPass", user.getPassword());
        assertTrue(passwordEncoder.matches("brandNewPass", user.getPassword()));
        verify(userDao).update(user);
    }

    @Test
    void changePassword_throwsAuthentication_whenOldPasswordIsWrong() {
        User user = userWithPassword("oldPass123");
        String hashBefore = user.getPassword();
        when(userDao.findByUsername("John.Doe")).thenReturn(Optional.of(user));

        assertThrows(AuthenticationException.class,
                () -> service.changePassword("John.Doe", "wrongOld", "brandNewPass"));

        assertEquals(hashBefore, user.getPassword());
        verify(userDao, never()).update(user);
    }

    @Test
    void changePassword_throwsAuthentication_whenUserDoesNotExist() {
        when(userDao.findByUsername("No.Body")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class,
                () -> service.changePassword("No.Body", "oldPass123", "brandNewPass"));
    }

    @Test
    void changePassword_throwsValidation_whenNewPasswordIsBlank() {
        assertThrows(ValidationException.class, () -> service.changePassword("John.Doe", "oldPass123", "  "));

        verifyNoInteractions(userDao);
    }

    @Test
    void changePassword_throwsValidation_whenOldPasswordIsMissing() {
        assertThrows(ValidationException.class, () -> service.changePassword("John.Doe", null, "brandNewPass"));

        verifyNoInteractions(userDao);
    }

    @Test
    void changePassword_isLogged_withoutLeakingPasswords() {
        User user = userWithPassword("oldPass123");
        when(userDao.findByUsername("John.Doe")).thenReturn(Optional.of(user));

        try (LogCapture logs = new LogCapture(UserAccountService.class)) {
            service.changePassword("John.Doe", "oldPass123", "brandNewPass");

            assertTrue(logs.messages(Level.INFO).stream().anyMatch(m -> m.contains("John.Doe")));
            assertTrue(logs.allMessages().stream()
                    .noneMatch(m -> m.contains("brandNewPass") || m.contains("oldPass123")
                            || m.contains(user.getPassword())));
        }
    }

    @Test
    void changePassword_failureIsLoggedAsWarning_withoutLeakingPasswords() {
        User user = userWithPassword("oldPass123");
        when(userDao.findByUsername("John.Doe")).thenReturn(Optional.of(user));

        try (LogCapture logs = new LogCapture(UserAccountService.class)) {
            assertThrows(AuthenticationException.class,
                    () -> service.changePassword("John.Doe", "wrongOld", "brandNewPass"));

            assertTrue(logs.messages(Level.WARN).stream().anyMatch(m -> m.contains("John.Doe")));
            assertTrue(logs.allMessages().stream()
                    .noneMatch(m -> m.contains("wrongOld") || m.contains("brandNewPass")));
        }
    }

    // ---------- setActive ----------

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
    void setActive_throwsConflict_whenAlreadyActive() {
        User user = User.builder().username("John.Doe").active(true).build();

        assertThrows(ConflictException.class, () -> service.setActive(user, true));

        verify(userDao, never()).update(user);
    }

    @Test
    void setActive_throwsConflict_whenAlreadyInactive() {
        User user = User.builder().username("John.Doe").active(false).build();

        assertThrows(ConflictException.class, () -> service.setActive(user, false));

        verify(userDao, never()).update(user);
    }
}