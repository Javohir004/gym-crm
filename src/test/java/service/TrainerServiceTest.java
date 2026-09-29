package service;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.dao.TrainingTypeDao;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.exception.NotFoundException;
import com.epam.training.gym.exception.ValidationException;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.TrainingType;
import com.epam.training.gym.model.User;
import com.epam.training.gym.service.AuthenticationService;
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
    private static final String PASSWORD = "pass123456";

    @Mock
    private TrainerDao trainerDao;
    @Mock
    private TrainingTypeDao trainingTypeDao;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private UserAccountService userAccountService;

    private TrainerService service;
    private User user;
    private Trainer trainer;
    private TrainingType yoga;
    private TrainingType fitness;

    @BeforeEach
    void setUp() {
        service = new TrainerService();
        service.setTrainerDao(trainerDao);
        service.setTrainingTypeDao(trainingTypeDao);
        service.setAuthenticationService(authenticationService);
        service.setUserAccountService(userAccountService);

        yoga = TrainingType.builder().id(2L).trainingTypeName("Yoga").build();
        fitness = TrainingType.builder().id(1L).trainingTypeName("Fitness").build();
        user = User.builder().firstName("Anna").lastName("Lee").username(USERNAME).password(PASSWORD).active(true).build();
        trainer = Trainer.builder().user(user).specialization(yoga).build();
    }


    @Test
    void createTrainer_savesTrainerWithSpecializationAndGeneratedUser() {
        when(trainingTypeDao.findByName("Yoga")).thenReturn(Optional.of(yoga));
        when(userAccountService.newUser("Anna", "Lee")).thenReturn(user);

        Trainer result = service.createTrainer("Anna", "Lee", "Yoga");

        ArgumentCaptor<Trainer> captor = ArgumentCaptor.forClass(Trainer.class);
        verify(trainerDao).save(captor.capture());
        assertSame(user, captor.getValue().getUser());
        assertSame(yoga, captor.getValue().getSpecialization());
        assertSame(captor.getValue(), result);
    }

    @Test
    void createTrainer_trimsSpecializationName() {
        when(trainingTypeDao.findByName("Yoga")).thenReturn(Optional.of(yoga));
        when(userAccountService.newUser("Anna", "Lee")).thenReturn(user);

        Trainer result = service.createTrainer("Anna", "Lee", "  Yoga ");

        assertSame(yoga, result.getSpecialization());
    }

    @Test
    void createTrainer_throwsNotFound_andCreatesNoUser_whenSpecializationIsUnknown() {
        when(trainingTypeDao.findByName("Karate")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.createTrainer("Anna", "Lee", "Karate"));

        assertTrue(ex.getMessage().contains("Karate"));
        verifyNoInteractions(userAccountService, trainerDao);
    }

    @Test
    void createTrainer_throwsValidation_whenSpecializationIsBlank() {
        assertThrows(ValidationException.class, () -> service.createTrainer("Anna", "Lee", " "));

        verifyNoInteractions(trainingTypeDao, userAccountService, trainerDao);
    }

    @Test
    void createTrainer_logsCreation_withoutLeakingPassword() {
        when(trainingTypeDao.findByName("Yoga")).thenReturn(Optional.of(yoga));
        when(userAccountService.newUser("Anna", "Lee")).thenReturn(user);

        try (LogCapture logs = new LogCapture(TrainerService.class)) {
            service.createTrainer("Anna", "Lee", "Yoga");

            assertTrue(logs.messages(Level.INFO).stream().anyMatch(m -> m.contains(USERNAME)));
            assertTrue(logs.allMessages().stream().noneMatch(m -> m.contains(PASSWORD)));
        }
    }


    @Test
    void credentialsMatch_delegatesToAuthenticationService() {
        when(authenticationService.trainerCredentialsMatch(USERNAME, PASSWORD)).thenReturn(true);
        when(authenticationService.trainerCredentialsMatch(USERNAME, "wrong")).thenReturn(false);

        assertTrue(service.credentialsMatch(USERNAME, PASSWORD));
        assertFalse(service.credentialsMatch(USERNAME, "wrong"));
    }

    @Test
    void getByUsername_returnsAuthenticatedTrainer() {
        when(authenticationService.authenticateTrainer(USERNAME, PASSWORD)).thenReturn(trainer);

        assertSame(trainer, service.getByUsername(USERNAME, PASSWORD));
    }

    @Test
    void getByUsername_propagatesAuthenticationFailure() {
        when(authenticationService.authenticateTrainer(USERNAME, "wrong"))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        assertThrows(AuthenticationException.class, () -> service.getByUsername(USERNAME, "wrong"));
    }

    @Test
    void changePassword_delegatesToUserAccountService() {
        when(authenticationService.authenticateTrainer(USERNAME, PASSWORD)).thenReturn(trainer);

        service.changePassword(USERNAME, PASSWORD, "newPass");

        verify(userAccountService).changePassword(user, "newPass");
    }

    @Test
    void toggleActive_returnsNewStateFromUserAccountService() {
        when(authenticationService.authenticateTrainer(USERNAME, PASSWORD)).thenReturn(trainer);
        when(userAccountService.toggleActive(user)).thenReturn(false);

        assertFalse(service.toggleActive(USERNAME, PASSWORD));
    }

    @Test
    void toggleActive_doesNothing_whenAuthenticationFails() {
        when(authenticationService.authenticateTrainer(USERNAME, "wrong"))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        assertThrows(AuthenticationException.class, () -> service.toggleActive(USERNAME, "wrong"));

        verifyNoInteractions(userAccountService);
    }


    @Test
    void update_changesNamesAndSpecialization() {
        when(authenticationService.authenticateTrainer(USERNAME, PASSWORD)).thenReturn(trainer);
        when(trainingTypeDao.findByName("Fitness")).thenReturn(Optional.of(fitness));
        when(trainerDao.update(trainer)).thenReturn(trainer);

        Trainer result = service.update(USERNAME, PASSWORD, " Anne ", " Lea ", " Fitness ");

        assertSame(trainer, result);
        assertEquals("Anne", user.getFirstName());
        assertEquals("Lea", user.getLastName());
        assertSame(fitness, trainer.getSpecialization());
        verify(trainerDao).update(trainer);
    }

    @Test
    void update_throwsValidation_whenAnyFieldIsBlank() {
        when(authenticationService.authenticateTrainer(USERNAME, PASSWORD)).thenReturn(trainer);

        assertThrows(ValidationException.class, () -> service.update(USERNAME, PASSWORD, "", "Lea", "Fitness"));
        assertThrows(ValidationException.class, () -> service.update(USERNAME, PASSWORD, "Anne", null, "Fitness"));
        assertThrows(ValidationException.class, () -> service.update(USERNAME, PASSWORD, "Anne", "Lea", " "));

        assertEquals("Anna", user.getFirstName());
        verify(trainerDao, never()).update(any());
    }

    @Test
    void update_throwsNotFound_whenSpecializationIsUnknown() {
        when(authenticationService.authenticateTrainer(USERNAME, PASSWORD)).thenReturn(trainer);
        when(trainingTypeDao.findByName("Karate")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(USERNAME, PASSWORD, "Anne", "Lea", "Karate"));

        verify(trainerDao, never()).update(any());
    }

    @Test
    void update_throws_whenAuthenticationFails() {
        when(authenticationService.authenticateTrainer(USERNAME, "wrong"))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        assertThrows(AuthenticationException.class,
                () -> service.update(USERNAME, "wrong", "Anne", "Lea", "Fitness"));

        verifyNoInteractions(trainerDao, trainingTypeDao);
    }
}