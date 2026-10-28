package service;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.dao.TraineeDao;
import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.exception.NotFoundException;
import com.epam.training.gym.exception.ValidationException;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.User;
import com.epam.training.gym.service.Registration;
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
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraineeServiceTest {

    private static final String USERNAME = "John.Doe";
    private static final String RAW_PASSWORD = "pass123456";
    private static final String PASSWORD_HASH = "$2a$10$hashedhashedhashedhashed";

    @Mock
    private TraineeDao traineeDao;
    @Mock
    private TrainerDao trainerDao;
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
        service.setUserAccountService(userAccountService);

        user = User.builder().firstName("John").lastName("Doe").username(USERNAME)
                .password(PASSWORD_HASH).active(true).build();
        trainee = Trainee.builder().user(user).dateOfBirth(LocalDate.of(2000, 1, 1)).address("Tashkent").build();
    }

    private static Trainer trainer(String username) {
        return Trainer.builder().user(User.builder().username(username).build()).build();
    }

    private void traineeExists() {
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.of(trainee));
    }

    // ---------- createTrainee ----------

    @Test
    void createTrainee_savesTraineeAndReturnsRawPasswordSeparately() {
        when(userAccountService.newUser("John", "Doe")).thenReturn(new Registration<>(user, RAW_PASSWORD));

        Registration<Trainee> result = service.createTrainee("John", "Doe", LocalDate.of(2000, 1, 1), "Tashkent");

        ArgumentCaptor<Trainee> captor = ArgumentCaptor.forClass(Trainee.class);
        verify(traineeDao).save(captor.capture());
        Trainee saved = captor.getValue();
        assertSame(user, saved.getUser());
        assertEquals(LocalDate.of(2000, 1, 1), saved.getDateOfBirth());
        assertEquals("Tashkent", saved.getAddress());
        assertSame(saved, result.profile());
        assertEquals(RAW_PASSWORD, result.rawPassword());
        assertEquals(PASSWORD_HASH, saved.getUser().getPassword());
    }

    @Test
    void createTrainee_doesNotSave_whenUserCreationFails() {
        when(userAccountService.newUser(" ", "Doe")).thenThrow(new ValidationException("First name must not be empty"));

        assertThrows(ValidationException.class, () -> service.createTrainee(" ", "Doe", null, null));

        verifyNoInteractions(traineeDao);
    }

    @Test
    void createTrainee_logsCreation_withoutLeakingPassword() {
        when(userAccountService.newUser("John", "Doe")).thenReturn(new Registration<>(user, RAW_PASSWORD));

        try (LogCapture logs = new LogCapture(TraineeService.class)) {
            service.createTrainee("John", "Doe", null, null);

            assertTrue(logs.messages(Level.INFO).stream().anyMatch(m -> m.contains(USERNAME)));
            assertTrue(logs.allMessages().stream()
                    .noneMatch(m -> m.contains(RAW_PASSWORD) || m.contains(PASSWORD_HASH)));
        }
    }

    // ---------- getByUsername ----------

    @Test
    void getByUsername_returnsTrainee() {
        traineeExists();

        assertSame(trainee, service.getByUsername(USERNAME));
    }

    @Test
    void getByUsername_throwsNotFound_whenTraineeDoesNotExist() {
        when(traineeDao.findByUsername("No.Body")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> service.getByUsername("No.Body"));

        assertTrue(ex.getMessage().contains("No.Body"));
    }

    // ---------- updateFullProfile ----------

    @Test
    void updateFullProfile_changesProfileAndActiveState() {
        traineeExists();
        when(traineeDao.update(trainee)).thenReturn(trainee);

        Trainee result = service.updateFullProfile(USERNAME, " Jack ", " Smith ",
                LocalDate.of(1999, 5, 20), "Samarkand", false);

        assertSame(trainee, result);
        assertEquals("Jack", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals(LocalDate.of(1999, 5, 20), trainee.getDateOfBirth());
        assertEquals("Samarkand", trainee.getAddress());
        assertFalse(user.isActive());
        verify(traineeDao).update(trainee);
    }

    @Test
    void updateFullProfile_throws_whenFirstNameIsBlank_andLeavesStateUntouched() {
        traineeExists();

        assertThrows(ValidationException.class,
                () -> service.updateFullProfile(USERNAME, " ", "Smith", null, null, false));

        assertEquals("John", user.getFirstName());
        assertTrue(user.isActive());
        verify(traineeDao, never()).update(any());
    }

    @Test
    void updateFullProfile_throws_whenLastNameIsBlank() {
        traineeExists();

        assertThrows(ValidationException.class,
                () -> service.updateFullProfile(USERNAME, "Jack", "", null, null, true));

        verify(traineeDao, never()).update(any());
    }

    @Test
    void updateFullProfile_throwsNotFound_whenTraineeDoesNotExist() {
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.updateFullProfile(USERNAME, "Jack", "Smith", null, null, true));

        verify(traineeDao, never()).update(any());
    }

    // ---------- setActive ----------

    @Test
    void setActive_delegatesToUserAccountService() {
        traineeExists();

        service.setActive(USERNAME, false);

        verify(userAccountService).setActive(user, false);
    }

    @Test
    void setActive_throwsNotFound_whenTraineeDoesNotExist() {
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.setActive(USERNAME, true));

        verifyNoInteractions(userAccountService);
    }

    // ---------- delete ----------

    @Test
    void delete_removesTrainee_andLogsIt() {
        traineeExists();

        try (LogCapture logs = new LogCapture(TraineeService.class)) {
            service.delete(USERNAME);

            assertTrue(logs.messages(Level.INFO).stream().anyMatch(m -> m.contains(USERNAME)));
        }
        verify(traineeDao).delete(trainee);
    }

    @Test
    void delete_throwsNotFound_andDeletesNothing_whenTraineeDoesNotExist() {
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete(USERNAME));

        verify(traineeDao, never()).delete(any());
    }

    // ---------- getUnassignedActiveTrainers ----------

    @Test
    void getUnassignedActiveTrainers_returnsDaoResult() {
        List<Trainer> expected = List.of(trainer("Anna.Lee"));
        traineeExists();
        when(trainerDao.findNotAssignedToTraineeActive(USERNAME)).thenReturn(expected);

        assertSame(expected, service.getUnassignedActiveTrainers(USERNAME));
    }

    @Test
    void getUnassignedActiveTrainers_throwsNotFound_andDoesNotQuery_whenTraineeDoesNotExist() {
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getUnassignedActiveTrainers(USERNAME));

        verifyNoInteractions(trainerDao);
    }

    // ---------- updateTrainers ----------

    @Test
    void updateTrainers_replacesTheWholeTrainersList() {
        Trainer oldTrainer = trainer("Old.Trainer");
        Trainer anna = trainer("Anna.Lee");
        Trainer mark = trainer("Mark.King");
        trainee.setTrainers(new HashSet<>(Set.of(oldTrainer)));
        traineeExists();
        when(trainerDao.findAllByUsernames(anyCollection())).thenReturn(List.of(anna, mark));
        when(traineeDao.update(trainee)).thenReturn(trainee);

        Trainee result = service.updateTrainers(USERNAME, List.of("Anna.Lee", "Mark.King"));

        assertSame(trainee, result);
        assertEquals(Set.of(anna, mark), trainee.getTrainers());
        assertFalse(trainee.getTrainers().contains(oldTrainer));
    }

    @Test
    void updateTrainers_ignoresDuplicateUsernames() {
        Trainer anna = trainer("Anna.Lee");
        traineeExists();
        when(trainerDao.findAllByUsernames(anyCollection())).thenReturn(List.of(anna));
        when(traineeDao.update(trainee)).thenReturn(trainee);

        service.updateTrainers(USERNAME, List.of("Anna.Lee", "Anna.Lee"));

        assertEquals(Set.of(anna), trainee.getTrainers());
    }

    @Test
    void updateTrainers_canClearTheList_withEmptyCollection() {
        trainee.setTrainers(new HashSet<>(Set.of(trainer("Old.Trainer"))));
        traineeExists();
        when(trainerDao.findAllByUsernames(anyCollection())).thenReturn(List.of());
        when(traineeDao.update(trainee)).thenReturn(trainee);

        service.updateTrainers(USERNAME, List.of());

        assertTrue(trainee.getTrainers().isEmpty());
    }

    @Test
    void updateTrainers_throwsNotFound_listingMissingUsernames_andKeepsOldList() {
        Trainer existing = trainer("Old.Trainer");
        trainee.setTrainers(new HashSet<>(Set.of(existing)));
        traineeExists();
        when(trainerDao.findAllByUsernames(anyCollection())).thenReturn(List.of(trainer("Anna.Lee")));

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.updateTrainers(USERNAME, List.of("Anna.Lee", "No.Such")));

        assertTrue(ex.getMessage().contains("No.Such"));
        assertFalse(ex.getMessage().contains("Anna.Lee"));
        assertEquals(Set.of(existing), trainee.getTrainers());
        verify(traineeDao, never()).update(any());
    }

    @Test
    void updateTrainers_throwsValidation_whenListIsNull() {
        traineeExists();

        assertThrows(ValidationException.class, () -> service.updateTrainers(USERNAME, null));

        verifyNoInteractions(trainerDao);
    }

    @Test
    void updateTrainers_logsNumberOfAssignedTrainers() {
        traineeExists();
        when(trainerDao.findAllByUsernames(anyCollection())).thenReturn(List.of(trainer("Anna.Lee")));
        when(traineeDao.update(trainee)).thenReturn(trainee);

        try (LogCapture logs = new LogCapture(TraineeService.class)) {
            service.updateTrainers(USERNAME, List.of("Anna.Lee"));

            assertTrue(logs.messages(Level.INFO).stream().anyMatch(m -> m.contains(USERNAME) && m.contains("1")));
        }
    }
}