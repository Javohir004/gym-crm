package service;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.dao.TraineeDao;
import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.exception.NotFoundException;
import com.epam.training.gym.exception.ValidationException;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.User;
import com.epam.training.gym.service.AuthenticationService;
import com.epam.training.gym.service.TraineeService;
import com.epam.training.gym.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import support.LogCapture;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraineeServiceTest {

    private static final String USERNAME = "John.Doe";
    private static final String PASSWORD = "pass123456";

    @Mock
    private TraineeDao traineeDao;
    @Mock
    private TrainerDao trainerDao;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private UserAccountService userAccountService;

    private TraineeService service;
    private User user;
    private Trainee trainee;

    @BeforeEach
    void setUp() {
        service = new TraineeService();
        service.setTraineeDao(traineeDao);
        service.setTrainerDao(trainerDao);
        service.setAuthenticationService(authenticationService);
        service.setUserAccountService(userAccountService);

        user = User.builder().firstName("John").lastName("Doe").username(USERNAME).password(PASSWORD).active(true).build();
        trainee = Trainee.builder().user(user).dateOfBirth(LocalDate.of(2000, 1, 1)).address("Tashkent").build();
    }

    private static Trainer trainer(String username) {
        return Trainer.builder()
                .user(User.builder().username(username).build())
                .build();
    }

    // ---------- create ----------

    @Test
    void createTrainee_savesTraineeWithGeneratedUser() {
        when(userAccountService.newUser("John", "Doe")).thenReturn(user);

        Trainee result = service.createTrainee("John", "Doe", LocalDate.of(2000, 1, 1), "Tashkent");

        ArgumentCaptor<Trainee> captor = ArgumentCaptor.forClass(Trainee.class);
        verify(traineeDao).save(captor.capture());
        Trainee saved = captor.getValue();
        assertSame(user, saved.getUser());
        assertEquals(LocalDate.of(2000, 1, 1), saved.getDateOfBirth());
        assertEquals("Tashkent", saved.getAddress());
        assertSame(saved, result);
    }

    @Test
    void createTrainee_doesNotSave_whenUserCreationFails() {
        when(userAccountService.newUser(" ", "Doe")).thenThrow(new ValidationException("First name must not be empty"));

        assertThrows(ValidationException.class, () -> service.createTrainee(" ", "Doe", null, null));

        verifyNoInteractions(traineeDao);
    }

    @Test
    void createTrainee_logsCreation_withoutLeakingPassword() {
        when(userAccountService.newUser("John", "Doe")).thenReturn(user);

        try (LogCapture logs = new LogCapture(TraineeService.class)) {
            service.createTrainee("John", "Doe", null, null);

            assertTrue(logs.messages(Level.INFO).stream().anyMatch(m -> m.contains(USERNAME)));
            assertTrue(logs.allMessages().stream().noneMatch(m -> m.contains(PASSWORD)));
        }
    }

    // ---------- authentication-backed reads ----------

    @Test
    void credentialsMatch_delegatesToAuthenticationService() {
        when(authenticationService.traineeCredentialsMatch(USERNAME, PASSWORD)).thenReturn(true);
        when(authenticationService.traineeCredentialsMatch(USERNAME, "wrong")).thenReturn(false);

        assertTrue(service.credentialsMatch(USERNAME, PASSWORD));
        assertFalse(service.credentialsMatch(USERNAME, "wrong"));
    }

    @Test
    void getByUsername_returnsAuthenticatedTrainee() {
        when(authenticationService.authenticateTrainee(USERNAME, PASSWORD)).thenReturn(trainee);

        assertSame(trainee, service.getByUsername(USERNAME, PASSWORD));
    }

    @Test
    void getByUsername_propagatesAuthenticationFailure() {
        when(authenticationService.authenticateTrainee(USERNAME, "wrong"))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        assertThrows(AuthenticationException.class, () -> service.getByUsername(USERNAME, "wrong"));
    }

    // ---------- change password / toggle ----------

    @Test
    void changePassword_delegatesToUserAccountService() {
        when(authenticationService.authenticateTrainee(USERNAME, PASSWORD)).thenReturn(trainee);

        service.changePassword(USERNAME, PASSWORD, "newPass");

        verify(userAccountService).changePassword(user, "newPass");
    }

    @Test
    void changePassword_doesNothing_whenAuthenticationFails() {
        when(authenticationService.authenticateTrainee(USERNAME, "wrong"))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        assertThrows(AuthenticationException.class, () -> service.changePassword(USERNAME, "wrong", "newPass"));

        verifyNoInteractions(userAccountService);
    }

    @Test
    void toggleActive_returnsNewStateFromUserAccountService() {
        when(authenticationService.authenticateTrainee(USERNAME, PASSWORD)).thenReturn(trainee);
        when(userAccountService.toggleActive(user)).thenReturn(false);

        assertFalse(service.toggleActive(USERNAME, PASSWORD));
    }

    // ---------- update ----------

    @Test
    void update_changesProfileAndPersistsIt() {
        when(authenticationService.authenticateTrainee(USERNAME, PASSWORD)).thenReturn(trainee);
        when(traineeDao.update(trainee)).thenReturn(trainee);

        Trainee result = service.update(USERNAME, PASSWORD, "  Jack ", " Smith ", LocalDate.of(1999, 5, 20), "Samarkand");

        assertSame(trainee, result);
        assertEquals("Jack", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals(LocalDate.of(1999, 5, 20), trainee.getDateOfBirth());
        assertEquals("Samarkand", trainee.getAddress());
        verify(traineeDao).update(trainee);
    }

    @Test
    void update_throws_whenFirstNameIsBlank_andLeavesProfileUntouched() {
        when(authenticationService.authenticateTrainee(USERNAME, PASSWORD)).thenReturn(trainee);

        assertThrows(ValidationException.class,
                () -> service.update(USERNAME, PASSWORD, " ", "Smith", null, "Somewhere"));

        assertEquals("John", user.getFirstName());
        assertEquals("Tashkent", trainee.getAddress());
        verify(traineeDao, never()).update(any());
    }

    @Test
    void update_throws_whenLastNameIsBlank() {
        when(authenticationService.authenticateTrainee(USERNAME, PASSWORD)).thenReturn(trainee);

        assertThrows(ValidationException.class,
                () -> service.update(USERNAME, PASSWORD, "Jack", null, null, null));

        verify(traineeDao, never()).update(any());
    }

    @Test
    void update_throws_whenAuthenticationFails() {
        when(authenticationService.authenticateTrainee(USERNAME, "wrong"))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        assertThrows(AuthenticationException.class,
                () -> service.update(USERNAME, "wrong", "Jack", "Smith", null, null));

        verifyNoInteractions(traineeDao);
    }

    // ---------- delete ----------

    @Test
    void delete_removesAuthenticatedTrainee() {
        when(authenticationService.authenticateTrainee(USERNAME, PASSWORD)).thenReturn(trainee);

        service.delete(USERNAME, PASSWORD);

        verify(traineeDao).delete(trainee);
    }

    @Test
    void delete_doesNotDelete_whenAuthenticationFails() {
        when(authenticationService.authenticateTrainee(USERNAME, "wrong"))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        assertThrows(AuthenticationException.class, () -> service.delete(USERNAME, "wrong"));

        verifyNoInteractions(traineeDao);
    }

    @Test
    void delete_logsDeletion() {
        when(authenticationService.authenticateTrainee(USERNAME, PASSWORD)).thenReturn(trainee);

        try (LogCapture logs = new LogCapture(TraineeService.class)) {
            service.delete(USERNAME, PASSWORD);

            assertTrue(logs.messages(Level.INFO).stream().anyMatch(m -> m.contains("deleted") && m.contains(USERNAME)));
        }
    }

    // ---------- unassigned trainers ----------

    @Test
    void getUnassignedTrainers_returnsTrainersFromDao_afterAuthentication() {
        List<Trainer> expected = List.of(trainer("Anna.Lee"));
        when(trainerDao.findNotAssignedToTrainee(USERNAME)).thenReturn(expected);

        List<Trainer> result = service.getUnassignedTrainers(USERNAME, PASSWORD);

        assertSame(expected, result);
        verify(authenticationService).authenticateTrainee(USERNAME, PASSWORD);
    }

    @Test
    void getUnassignedTrainers_doesNotQuery_whenAuthenticationFails() {
        when(authenticationService.authenticateTrainee(USERNAME, "wrong"))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        assertThrows(AuthenticationException.class, () -> service.getUnassignedTrainers(USERNAME, "wrong"));

        verifyNoInteractions(trainerDao);
    }

    // ---------- update trainers ----------

    @Test
    void updateTrainers_replacesTheWholeTrainersList() {
        Trainer oldTrainer = trainer("Old.Trainer");
        Trainer anna = trainer("Anna.Lee");
        Trainer mark = trainer("Mark.King");
        trainee.setTrainers(new HashSet<>(Set.of(oldTrainer)));
        when(authenticationService.authenticateTrainee(USERNAME, PASSWORD)).thenReturn(trainee);
        when(trainerDao.findAllByUsernames(anyCollection())).thenReturn(List.of(anna, mark));
        when(traineeDao.update(trainee)).thenReturn(trainee);

        Trainee result = service.updateTrainers(USERNAME, PASSWORD, List.of("Anna.Lee", "Mark.King"));

        assertSame(trainee, result);
        assertEquals(Set.of(anna, mark), trainee.getTrainers());
        assertFalse(trainee.getTrainers().contains(oldTrainer));
    }

    @Test
    void updateTrainers_ignoresDuplicateUsernames() {
        Trainer anna = trainer("Anna.Lee");
        when(authenticationService.authenticateTrainee(USERNAME, PASSWORD)).thenReturn(trainee);
        when(trainerDao.findAllByUsernames(anyCollection())).thenReturn(List.of(anna));
        when(traineeDao.update(trainee)).thenReturn(trainee);

        service.updateTrainers(USERNAME, PASSWORD, List.of("Anna.Lee", "Anna.Lee"));

        assertEquals(Set.of(anna), trainee.getTrainers());
    }

    @Test
    void updateTrainers_canClearTheList_withEmptyCollection() {
        trainee.setTrainers(new HashSet<>(Set.of(trainer("Old.Trainer"))));
        when(authenticationService.authenticateTrainee(USERNAME, PASSWORD)).thenReturn(trainee);
        when(trainerDao.findAllByUsernames(anyCollection())).thenReturn(List.of());
        when(traineeDao.update(trainee)).thenReturn(trainee);

        service.updateTrainers(USERNAME, PASSWORD, List.of());

        assertTrue(trainee.getTrainers().isEmpty());
    }

    @Test
    void updateTrainers_throwsNotFound_listingMissingUsernames_andKeepsOldList() {
        Trainer existing = trainer("Old.Trainer");
        trainee.setTrainers(new HashSet<>(Set.of(existing)));
        when(authenticationService.authenticateTrainee(USERNAME, PASSWORD)).thenReturn(trainee);
        when(trainerDao.findAllByUsernames(anyCollection())).thenReturn(List.of(trainer("Anna.Lee")));

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.updateTrainers(USERNAME, PASSWORD, List.of("Anna.Lee", "No.Such")));

        assertTrue(ex.getMessage().contains("No.Such"));
        assertFalse(ex.getMessage().contains("Anna.Lee"));
        assertEquals(Set.of(existing), trainee.getTrainers());
        verify(traineeDao, never()).update(any());
    }

    @Test
    void updateTrainers_throwsValidation_whenListIsNull() {
        when(authenticationService.authenticateTrainee(USERNAME, PASSWORD)).thenReturn(trainee);

        assertThrows(ValidationException.class, () -> service.updateTrainers(USERNAME, PASSWORD, null));

        verifyNoInteractions(trainerDao);
    }

    @Test
    void updateTrainers_logsNumberOfAssignedTrainers() {
        when(authenticationService.authenticateTrainee(USERNAME, PASSWORD)).thenReturn(trainee);
        when(trainerDao.findAllByUsernames(anyCollection())).thenReturn(List.of(trainer("Anna.Lee")));
        when(traineeDao.update(trainee)).thenReturn(trainee);

        try (LogCapture logs = new LogCapture(TraineeService.class)) {
            service.updateTrainers(USERNAME, PASSWORD, List.of("Anna.Lee"));

            assertTrue(logs.messages(Level.INFO).stream().anyMatch(m -> m.contains(USERNAME) && m.contains("1")));
        }
    }
}