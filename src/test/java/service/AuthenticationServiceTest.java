package service;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final String USERNAME = "Jane.Doe";
    private static final String PASSWORD = "correct123";

    // low cost factor keeps the tests fast; production uses the BCrypt default
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);
    private static final String PASSWORD_HASH = ENCODER.encode(PASSWORD);

    @Mock
    private UserDao userDao;
    @Mock
    private TraineeDao traineeDao;
    @Mock
    private TrainerDao trainerDao;

    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticationService();
        service.setUserDao(userDao);
        service.setTraineeDao(traineeDao);
        service.setTrainerDao(trainerDao);
        service.setPasswordEncoder(ENCODER);
    }

    private static User user() {
        return User.builder().username(USERNAME).password(PASSWORD_HASH).build();
    }

    @Test
    void authenticate_returnsUser_whenPasswordMatchesStoredHash() {
        User user = user();
        when(userDao.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        assertSame(user, service.authenticate(USERNAME, PASSWORD));
    }

    @Test
    void authenticate_throws_whenPasswordIsWrong() {
        when(userDao.findByUsername(USERNAME)).thenReturn(Optional.of(user()));

        assertThrows(AuthenticationException.class, () -> service.authenticate(USERNAME, "wrong"));
    }

    @Test
    void authenticate_throws_whenTheHashItselfIsSentAsPassword() {
        when(userDao.findByUsername(USERNAME)).thenReturn(Optional.of(user()));

        assertThrows(AuthenticationException.class, () -> service.authenticate(USERNAME, PASSWORD_HASH));
    }

    @Test
    void authenticate_throws_whenUserDoesNotExist() {
        when(userDao.findByUsername("No.One")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () -> service.authenticate("No.One", PASSWORD));
    }

    @Test
    void authenticate_throws_withoutDbLookup_whenCredentialsAreBlank() {
        assertThrows(AuthenticationException.class, () -> service.authenticate(" ", PASSWORD));
        assertThrows(AuthenticationException.class, () -> service.authenticate(USERNAME, null));

        verifyNoInteractions(userDao);
    }

    @Test
    void authenticateTrainee_returnsTrainee_whenPasswordMatches() {
        Trainee trainee = Trainee.builder().user(user()).build();
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.of(trainee));

        assertSame(trainee, service.authenticateTrainee(USERNAME, PASSWORD));
    }

    @Test
    void authenticateTrainee_throws_whenPasswordIsWrong() {
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.of(Trainee.builder().user(user()).build()));

        assertThrows(AuthenticationException.class, () -> service.authenticateTrainee(USERNAME, "wrong"));
    }

    @Test
    void authenticateTrainer_returnsTrainer_whenPasswordMatches() {
        Trainer trainer = Trainer.builder().user(user()).build();
        when(trainerDao.findByUsername(USERNAME)).thenReturn(Optional.of(trainer));

        assertSame(trainer, service.authenticateTrainer(USERNAME, PASSWORD));
    }

    @Test
    void authenticateTrainer_throws_whenNotATrainer() {
        when(trainerDao.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () -> service.authenticateTrainer(USERNAME, PASSWORD));
    }

    @Test
    void traineeCredentialsMatch_isTrueOnlyForTheRightPassword() {
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.of(Trainee.builder().user(user()).build()));

        assertTrue(service.traineeCredentialsMatch(USERNAME, PASSWORD));
        assertFalse(service.traineeCredentialsMatch(USERNAME, "wrong"));
    }

    @Test
    void trainerCredentialsMatch_isFalseForBlankPassword_withoutDbLookup() {
        assertFalse(service.trainerCredentialsMatch(USERNAME, " "));

        verifyNoInteractions(trainerDao);
    }

    @Test
    void trainerCredentialsMatch_isTrueOnlyForTheRightPassword() {
        when(trainerDao.findByUsername(USERNAME)).thenReturn(Optional.of(Trainer.builder().user(user()).build()));

        assertTrue(service.trainerCredentialsMatch(USERNAME, PASSWORD));
        assertFalse(service.trainerCredentialsMatch(USERNAME, "wrong"));
    }
}
