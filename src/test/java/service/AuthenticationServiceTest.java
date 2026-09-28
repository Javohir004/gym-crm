package service;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.dao.TraineeDao;
import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.dao.UserDao;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.User;
import com.epam.training.gym.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import support.LogCapture;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final String USERNAME = "John.Doe";
    private static final String PASSWORD = "s3cretPass";

    @Mock
    private UserDao userDao;
    @Mock
    private TraineeDao traineeDao;
    @Mock
    private TrainerDao trainerDao;

    private AuthenticationService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AuthenticationService();
        service.setUserDao(userDao);
        service.setTraineeDao(traineeDao);
        service.setTrainerDao(trainerDao);
        user = User.builder().username(USERNAME).password(PASSWORD).active(true).build();
    }

    // ---------- authenticate(User) ----------

    @Test
    void authenticate_returnsUser_whenCredentialsMatch() {
        when(userDao.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        assertSame(user, service.authenticate(USERNAME, PASSWORD));
    }

    @Test
    void authenticate_throws_whenPasswordIsWrong() {
        when(userDao.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        assertThrows(AuthenticationException.class, () -> service.authenticate(USERNAME, "wrong"));
    }

    @Test
    void authenticate_throws_whenUserDoesNotExist() {
        when(userDao.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () -> service.authenticate("ghost", PASSWORD));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void authenticate_throwsWithoutTouchingDatabase_whenUsernameIsBlank(String blank) {
        assertThrows(AuthenticationException.class, () -> service.authenticate(blank, PASSWORD));
        verifyNoInteractions(userDao);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void authenticate_throwsWithoutTouchingDatabase_whenPasswordIsBlank(String blank) {
        assertThrows(AuthenticationException.class, () -> service.authenticate(USERNAME, blank));
        verifyNoInteractions(userDao);
    }

    @Test
    void authenticate_doesNotRevealWhetherUsernameOrPasswordWasWrong() {
        when(userDao.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(userDao.findByUsername("ghost")).thenReturn(Optional.empty());

        AuthenticationException wrongPassword =
                assertThrows(AuthenticationException.class, () -> service.authenticate(USERNAME, "wrong"));
        AuthenticationException unknownUser =
                assertThrows(AuthenticationException.class, () -> service.authenticate("ghost", "wrong"));

        assertEquals(wrongPassword.getMessage(), unknownUser.getMessage());
    }

    // ---------- trainee ----------

    @Test
    void authenticateTrainee_returnsTrainee_whenCredentialsMatch() {
        Trainee trainee = Trainee.builder().user(user).build();
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.of(trainee));

        assertSame(trainee, service.authenticateTrainee(USERNAME, PASSWORD));
    }

    @Test
    void authenticateTrainee_throws_whenPasswordIsWrong() {
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.of(Trainee.builder().user(user).build()));

        assertThrows(AuthenticationException.class, () -> service.authenticateTrainee(USERNAME, "wrong"));
    }

    @Test
    void authenticateTrainee_throws_whenTraineeDoesNotExist() {
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () -> service.authenticateTrainee(USERNAME, PASSWORD));
    }

    @Test
    void authenticateTrainee_throwsWithoutTouchingDatabase_whenInputIsBlank() {
        assertThrows(AuthenticationException.class, () -> service.authenticateTrainee(null, PASSWORD));
        assertThrows(AuthenticationException.class, () -> service.authenticateTrainee(USERNAME, " "));
        verifyNoInteractions(traineeDao);
    }

    @Test
    void traineeCredentialsMatch_isTrue_onlyForCorrectPassword() {
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.of(Trainee.builder().user(user).build()));

        assertTrue(service.traineeCredentialsMatch(USERNAME, PASSWORD));
        assertFalse(service.traineeCredentialsMatch(USERNAME, "wrong"));
    }

    @Test
    void traineeCredentialsMatch_isFalse_forBlankInputAndUnknownUser() {
        when(traineeDao.findByUsername("ghost")).thenReturn(Optional.empty());

        assertFalse(service.traineeCredentialsMatch("", PASSWORD));
        assertFalse(service.traineeCredentialsMatch("ghost", PASSWORD));
    }

    // ---------- trainer ----------

    @Test
    void authenticateTrainer_returnsTrainer_whenCredentialsMatch() {
        Trainer trainer = Trainer.builder().user(user).build();
        when(trainerDao.findByUsername(USERNAME)).thenReturn(Optional.of(trainer));

        assertSame(trainer, service.authenticateTrainer(USERNAME, PASSWORD));
    }

    @Test
    void authenticateTrainer_throws_whenPasswordIsWrong() {
        when(trainerDao.findByUsername(USERNAME)).thenReturn(Optional.of(Trainer.builder().user(user).build()));

        assertThrows(AuthenticationException.class, () -> service.authenticateTrainer(USERNAME, "wrong"));
    }

    @Test
    void authenticateTrainer_throws_whenTrainerDoesNotExist() {
        when(trainerDao.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () -> service.authenticateTrainer(USERNAME, PASSWORD));
    }

    @Test
    void authenticateTrainer_throwsWithoutTouchingDatabase_whenInputIsBlank() {
        assertThrows(AuthenticationException.class, () -> service.authenticateTrainer(USERNAME, null));
        verifyNoInteractions(trainerDao);
    }

    @Test
    void trainerCredentialsMatch_isTrue_onlyForCorrectPassword() {
        when(trainerDao.findByUsername(USERNAME)).thenReturn(Optional.of(Trainer.builder().user(user).build()));

        assertTrue(service.trainerCredentialsMatch(USERNAME, PASSWORD));
        assertFalse(service.trainerCredentialsMatch(USERNAME, "wrong"));
    }

    @Test
    void trainerCredentialsMatch_isFalse_forBlankInput() {
        assertFalse(service.trainerCredentialsMatch(" ", PASSWORD));
        assertFalse(service.trainerCredentialsMatch(USERNAME, ""));
        verifyNoInteractions(trainerDao);
    }

    // ---------- logging ----------

    @Test
    void failedAuthentication_isLoggedAsWarning_withoutLeakingPassword() {
        when(userDao.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        try (LogCapture logs = new LogCapture(AuthenticationService.class)) {
            assertThrows(AuthenticationException.class, () -> service.authenticate(USERNAME, "attemptedSecret"));

            assertTrue(logs.messages(Level.WARN).stream().anyMatch(m -> m.contains(USERNAME)));
            assertTrue(logs.allMessages().stream().noneMatch(m -> m.contains("attemptedSecret")));
            assertTrue(logs.allMessages().stream().noneMatch(m -> m.contains(PASSWORD)));
        }
    }

    @Test
    void successfulAuthentication_isLoggedAtDebug_withoutLeakingPassword() {
        when(userDao.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        try (LogCapture logs = new LogCapture(AuthenticationService.class)) {
            service.authenticate(USERNAME, PASSWORD);

            assertTrue(logs.messages(Level.DEBUG).stream().anyMatch(m -> m.contains(USERNAME)));
            assertTrue(logs.messages(Level.WARN).isEmpty());
            assertTrue(logs.allMessages().stream().noneMatch(m -> m.contains(PASSWORD)));
        }
    }
}