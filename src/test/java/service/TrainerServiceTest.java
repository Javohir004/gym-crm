package service;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.dao.TrainingTypeDao;
import com.epam.training.gym.exception.NotFoundException;
import com.epam.training.gym.exception.ValidationException;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.TrainingType;
import com.epam.training.gym.model.User;
import com.epam.training.gym.service.Registration;
import com.epam.training.gym.service.TrainerService;
import com.epam.training.gym.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import support.LogCapture;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerServiceTest {

    private static final String USERNAME = "Anna.Lee";
    private static final String RAW_PASSWORD = "pass123456";
    private static final String PASSWORD_HASH = "$2a$10$hashedhashedhashedhashed";

    @Mock
    private TrainerDao trainerDao;
    @Mock
    private TrainingTypeDao trainingTypeDao;
    @Mock
    private UserAccountService userAccountService;

    private TrainerService service;
    private User user;
    private Trainer trainer;
    private TrainingType yoga;

    @BeforeEach
    void setUp() {
        service = new TrainerService();
        service.setTrainerDao(trainerDao);
        service.setTrainingTypeDao(trainingTypeDao);
        service.setUserAccountService(userAccountService);

        yoga = TrainingType.builder().trainingTypeName("Yoga").build();
        user = User.builder().firstName("Anna").lastName("Lee").username(USERNAME)
                .password(PASSWORD_HASH).active(true).build();
        trainer = Trainer.builder().user(user).specialization(yoga).build();
    }

    private void trainerExists() {
        when(trainerDao.findByUsername(USERNAME)).thenReturn(Optional.of(trainer));
    }

    // ---------- createTrainer ----------

    @Test
    void createTrainer_savesTrainerWithSpecialization_andReturnsRawPasswordSeparately() {
        when(trainingTypeDao.findByName("Yoga")).thenReturn(Optional.of(yoga));
        when(userAccountService.newUser("Anna", "Lee")).thenReturn(new Registration<>(user, RAW_PASSWORD));

        Registration<Trainer> result = service.createTrainer("Anna", "Lee", "Yoga");

        ArgumentCaptor<Trainer> captor = ArgumentCaptor.forClass(Trainer.class);
        verify(trainerDao).save(captor.capture());
        assertSame(user, captor.getValue().getUser());
        assertSame(yoga, captor.getValue().getSpecialization());
        assertSame(captor.getValue(), result.profile());
        assertEquals(RAW_PASSWORD, result.rawPassword());
    }

    @Test
    void createTrainer_trimsSpecializationName() {
        when(trainingTypeDao.findByName("Yoga")).thenReturn(Optional.of(yoga));
        when(userAccountService.newUser("Anna", "Lee")).thenReturn(new Registration<>(user, RAW_PASSWORD));

        service.createTrainer("Anna", "Lee", "  Yoga ");

        verify(trainingTypeDao).findByName("Yoga");
    }

    @Test
    void createTrainer_throwsNotFound_andCreatesNoUser_whenSpecializationIsUnknown() {
        when(trainingTypeDao.findByName("Chess")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.createTrainer("Anna", "Lee", "Chess"));

        verifyNoInteractions(userAccountService);
        verify(trainerDao, never()).save(any());
    }

    @Test
    void createTrainer_throwsValidation_whenSpecializationIsBlank() {
        assertThrows(ValidationException.class, () -> service.createTrainer("Anna", "Lee", " "));

        verifyNoInteractions(trainingTypeDao, userAccountService, trainerDao);
    }

    @Test
    void createTrainer_logsCreation_withoutLeakingPassword() {
        when(trainingTypeDao.findByName("Yoga")).thenReturn(Optional.of(yoga));
        when(userAccountService.newUser("Anna", "Lee")).thenReturn(new Registration<>(user, RAW_PASSWORD));

        try (LogCapture logs = new LogCapture(TrainerService.class)) {
            service.createTrainer("Anna", "Lee", "Yoga");

            assertTrue(logs.messages(Level.INFO).stream().anyMatch(m -> m.contains(USERNAME)));
            assertTrue(logs.allMessages().stream()
                    .noneMatch(m -> m.contains(RAW_PASSWORD) || m.contains(PASSWORD_HASH)));
        }
    }

    // ---------- getByUsername ----------

    @Test
    void getByUsername_returnsTrainer() {
        trainerExists();

        assertSame(trainer, service.getByUsername(USERNAME));
    }

    @Test
    void getByUsername_throwsNotFound_whenTrainerDoesNotExist() {
        when(trainerDao.findByUsername("No.Body")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> service.getByUsername("No.Body"));

        assertTrue(ex.getMessage().contains("No.Body"));
    }

    // ---------- updateFullProfile ----------

    @Test
    void updateFullProfile_changesNamesAndActiveState_butNeverSpecialization() {
        trainerExists();
        when(trainerDao.update(trainer)).thenReturn(trainer);

        Trainer result = service.updateFullProfile(USERNAME, " Mary ", " King ", false);

        assertSame(trainer, result);
        assertEquals("Mary", user.getFirstName());
        assertEquals("King", user.getLastName());
        assertFalse(user.isActive());
        assertSame(yoga, trainer.getSpecialization());
    }

    @Test
    void updateFullProfile_throws_whenFirstNameIsBlank() {
        trainerExists();

        assertThrows(ValidationException.class, () -> service.updateFullProfile(USERNAME, " ", "King", true));

        verify(trainerDao, never()).update(any());
    }

    @Test
    void updateFullProfile_throws_whenLastNameIsBlank() {
        trainerExists();

        assertThrows(ValidationException.class, () -> service.updateFullProfile(USERNAME, "Mary", "", true));

        verify(trainerDao, never()).update(any());
    }

    @Test
    void updateFullProfile_throwsNotFound_whenTrainerDoesNotExist() {
        when(trainerDao.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.updateFullProfile(USERNAME, "Mary", "King", true));
    }

    // ---------- setActive ----------

    @Test
    void setActive_delegatesToUserAccountService() {
        trainerExists();

        service.setActive(USERNAME, false);

        verify(userAccountService).setActive(user, false);
    }

    @Test
    void setActive_throwsNotFound_whenTrainerDoesNotExist() {
        when(trainerDao.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.setActive(USERNAME, true));

        verifyNoInteractions(userAccountService);
    }
}