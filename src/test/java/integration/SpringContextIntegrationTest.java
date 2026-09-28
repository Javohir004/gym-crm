package integration;

import com.epam.training.gym.config.AppConfig;
import com.epam.training.gym.dto.TrainingRequest;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.exception.NotFoundException;
import com.epam.training.gym.exception.ValidationException;
import com.epam.training.gym.facade.GymFacade;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test of the whole stack (facade -> services -> DAOs -> Hibernate -> H2).
 * The tests are ordered because they share the same data and follow one user story.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SpringContextIntegrationTest {

    private static AnnotationConfigApplicationContext context;
    private static GymFacade facade;

    private static String annaUser;
    private static String annaPass;
    private static String markUser;
    private static String johnUser;
    private static String johnPass;
    private static String john2User;
    private static String john2Pass;

    @BeforeAll
    static void startContext() {
        context = new AnnotationConfigApplicationContext(AppConfig.class);
        facade = context.getBean(GymFacade.class);
    }

    @AfterAll
    static void stopContext() {
        if (context != null) {
            context.close();
        }
    }

    private static List<String> names(List<Training> trainings) {
        return trainings.stream().map(Training::getTrainingName).toList();
    }

    private static List<String> usernames(List<Trainer> trainers) {
        return trainers.stream().map(t -> t.getUser().getUsername()).toList();
    }

    @Test
    @Order(1)
    void createProfiles_generatesUsernamesAndPasswords_withSerialForSameName() {
        Trainer anna = facade.createTrainer("Anna", "Lee", "Yoga");
        Trainer mark = facade.createTrainer("Mark", "King", "Fitness");
        Trainee john = facade.createTrainee("John", "Doe", LocalDate.of(2000, 1, 15), "Tashkent");
        Trainee john2 = facade.createTrainee("John", "Doe", LocalDate.of(1999, 5, 20), "Samarkand");

        annaUser = anna.getUser().getUsername();
        annaPass = anna.getUser().getPassword();
        markUser = mark.getUser().getUsername();
        johnUser = john.getUser().getUsername();
        johnPass = john.getUser().getPassword();
        john2User = john2.getUser().getUsername();
        john2Pass = john2.getUser().getPassword();

        assertEquals("Anna.Lee", annaUser);
        assertEquals("Mark.King", markUser);
        assertEquals("John.Doe", johnUser);
        assertEquals("John.Doe1", john2User);
        assertEquals(10, annaPass.length());
        assertTrue(anna.getUser().isActive());
        assertNotNull(anna.getId());
    }

    @Test
    @Order(2)
    void invalidInput_isRejected() {
        assertThrows(ValidationException.class, () -> facade.createTrainee(" ", "Doe", null, null));
        assertThrows(NotFoundException.class, () -> facade.createTrainer("Bob", "Ray", "Karate"));
    }

    @Test
    @Order(3)
    void authentication_acceptsCorrectAndRejectsWrongCredentials() {
        assertTrue(facade.traineeCredentialsMatch(johnUser, johnPass));
        assertFalse(facade.traineeCredentialsMatch(johnUser, "wrong-password"));
        assertTrue(facade.trainerCredentialsMatch(annaUser, annaPass));
        assertFalse(facade.trainerCredentialsMatch(johnUser, johnPass), "a trainee is not a trainer");

        assertThrows(AuthenticationException.class, () -> facade.getTrainee(johnUser, "wrong-password"));
        assertEquals("Tashkent", facade.getTrainee(johnUser, johnPass).getAddress());
        assertEquals("Yoga", facade.getTrainer(annaUser, annaPass).getSpecialization().getTrainingTypeName());
    }

    @Test
    @Order(4)
    void updateProfiles_persistsChanges() {
        facade.updateTrainer(annaUser, annaPass, "Anna", "Lee", "Stretching");
        facade.updateTrainee(johnUser, johnPass, "John", "Doe", LocalDate.of(2000, 1, 15), "Yunusobod");

        assertEquals("Stretching", facade.getTrainer(annaUser, annaPass).getSpecialization().getTrainingTypeName());
        assertEquals("Yunusobod", facade.getTrainee(johnUser, johnPass).getAddress());
    }

    @Test
    @Order(5)
    void changePassword_invalidatesOldPassword() {
        String oldJohnPass = johnPass;
        johnPass = "NewTraineePass1";
        facade.changeTraineePassword(johnUser, oldJohnPass, johnPass);

        assertFalse(facade.traineeCredentialsMatch(johnUser, oldJohnPass));
        assertTrue(facade.traineeCredentialsMatch(johnUser, johnPass));

        String oldAnnaPass = annaPass;
        annaPass = "NewTrainerPass1";
        facade.changeTrainerPassword(annaUser, oldAnnaPass, annaPass);

        assertFalse(facade.trainerCredentialsMatch(annaUser, oldAnnaPass));
        assertTrue(facade.trainerCredentialsMatch(annaUser, annaPass));
    }

    @Test
    @Order(6)
    void toggleActive_switchesStateOnEveryCall() {
        assertFalse(facade.toggleTraineeActive(johnUser, johnPass));
        assertTrue(facade.toggleTraineeActive(johnUser, johnPass));
        assertFalse(facade.toggleTrainerActive(annaUser, annaPass));
        assertTrue(facade.toggleTrainerActive(annaUser, annaPass));
    }

    @Test
    @Order(7)
    void traineeTrainersList_canBeReadAndReplaced() {
        assertEquals(List.of(annaUser, markUser).stream().sorted().toList(),
                usernames(facade.getUnassignedTrainers(johnUser, johnPass)).stream().sorted().toList());

        facade.updateTraineeTrainers(johnUser, johnPass, List.of(annaUser));

        assertEquals(List.of(markUser), usernames(facade.getUnassignedTrainers(johnUser, johnPass)));
        assertThrows(NotFoundException.class,
                () -> facade.updateTraineeTrainers(johnUser, johnPass, List.of("No.Such")));
        assertEquals(List.of(markUser), usernames(facade.getUnassignedTrainers(johnUser, johnPass)),
                "a failed update must not change the list");
    }

    @Test
    @Order(8)
    void addTraining_andSearchByCriteria() {
        facade.addTraining(johnUser, johnPass, new TrainingRequest(
                johnUser, annaUser, "Morning Yoga", "Yoga", LocalDate.of(2026, 9, 1), 60));
        facade.addTraining(johnUser, johnPass, new TrainingRequest(
                johnUser, markUser, "Strength Basics", "Fitness", LocalDate.of(2026, 9, 15), 45));
        facade.addTraining(john2User, john2Pass, new TrainingRequest(
                john2User, annaUser, "Evening Stretch", "Stretching", LocalDate.of(2026, 9, 20), 30));

        assertEquals(List.of("Morning Yoga", "Strength Basics"),
                names(facade.getTraineeTrainings(johnUser, johnPass, null, null, null, null)));
        assertEquals(List.of("Strength Basics"), names(facade.getTraineeTrainings(
                johnUser, johnPass, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 30), null, null)));
        assertEquals(List.of("Morning Yoga"),
                names(facade.getTraineeTrainings(johnUser, johnPass, null, null, "anna", null)));
        assertEquals(List.of("Strength Basics"),
                names(facade.getTraineeTrainings(johnUser, johnPass, null, null, null, "Fitness")));

        assertEquals(List.of("Morning Yoga", "Evening Stretch"),
                names(facade.getTrainerTrainings(annaUser, annaPass, null, null, null)));
        assertEquals(List.of("Evening Stretch"),
                names(facade.getTrainerTrainings(annaUser, annaPass, LocalDate.of(2026, 9, 15), null, null)));
    }

    @Test
    @Order(9)
    void addTraining_rejectsInvalidData() {
        assertThrows(ValidationException.class, () -> facade.addTraining(johnUser, johnPass, new TrainingRequest(
                johnUser, annaUser, "Bad", "Yoga", LocalDate.of(2026, 9, 1), 0)));
        assertThrows(NotFoundException.class, () -> facade.addTraining(johnUser, johnPass, new TrainingRequest(
                johnUser, "No.Such", "Bad", "Yoga", LocalDate.of(2026, 9, 1), 30)));
        assertThrows(AuthenticationException.class, () -> facade.addTraining(johnUser, "wrong", new TrainingRequest(
                johnUser, annaUser, "Bad", "Yoga", LocalDate.of(2026, 9, 1), 30)));
    }

    @Test
    @Order(10)
    void deleteTrainee_removesProfileAndItsTrainings() {
        facade.deleteTrainee(johnUser, johnPass);

        assertFalse(facade.traineeCredentialsMatch(johnUser, johnPass));
        assertThrows(AuthenticationException.class, () -> facade.getTrainee(johnUser, johnPass));
        assertEquals(List.of("Evening Stretch"),
                names(facade.getTrainerTrainings(annaUser, annaPass, null, null, null)),
                "only the other trainee's training must remain");
    }
}