package dao;

import com.epam.training.gym.config.AppConfig;
import com.epam.training.gym.dao.TraineeDao;
import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.dao.TrainingDao;
import com.epam.training.gym.dao.TrainingTypeDao;
import com.epam.training.gym.dao.UserDao;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import com.epam.training.gym.model.TrainingType;
import com.epam.training.gym.model.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class DaoIntegrationTest {

    private static AnnotationConfigApplicationContext context;
    private static TransactionTemplate tx;
    private static UserDao userDao;
    private static TraineeDao traineeDao;
    private static TrainerDao trainerDao;
    private static TrainingDao trainingDao;
    private static TrainingTypeDao trainingTypeDao;

    @BeforeAll
    static void startContext() {
        context = new AnnotationConfigApplicationContext(AppConfig.class);
        tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        userDao = context.getBean(UserDao.class);
        traineeDao = context.getBean(TraineeDao.class);
        trainerDao = context.getBean(TrainerDao.class);
        trainingDao = context.getBean(TrainingDao.class);
        trainingTypeDao = context.getBean(TrainingTypeDao.class);
    }

    @AfterAll
    static void stopContext() {
        if (context != null) {
            context.close();
        }
    }


    private static void rollingBack(Runnable body) {
        tx.executeWithoutResult(status -> {
            body.run();
            status.setRollbackOnly();
        });
    }

    private static User user(String first, String last, String username) {
        return User.builder().firstName(first).lastName(last).username(username).password("pw12345678").active(true).build();
    }

    private static Trainee saveTrainee(String first, String last, String username) {
        return traineeDao.save(Trainee.builder()
                .user(user(first, last, username))
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .address("Tashkent")
                .build());
    }

    private static Trainer saveTrainer(String first, String last, String username, String specialization) {
        TrainingType type = trainingTypeDao.findByName(specialization).orElseThrow();
        return trainerDao.save(Trainer.builder()
                .user(user(first, last, username))
                .specialization(type)
                .build());
    }

    private static Training saveTraining(Trainee trainee, Trainer trainer, String name, String type, LocalDate date) {
        return trainingDao.save(Training.builder()
                .trainee(trainee)
                .trainer(trainer)
                .trainingName(name)
                .trainingType(trainingTypeDao.findByName(type).orElseThrow())
                .trainingDate(date)
                .trainingDuration(45)
                .build());
    }

    private static List<String> names(List<Training> trainings) {
        return trainings.stream().map(Training::getTrainingName).toList();
    }

    // ---------- TrainingTypeDao ----------

    @Test
    void trainingTypeDao_findByName_returnsSeededType() {
        rollingBack(() -> {
            Optional<TrainingType> yoga = trainingTypeDao.findByName("Yoga");

            assertTrue(yoga.isPresent());
            assertEquals("Yoga", yoga.get().getTrainingTypeName());
        });
    }

    @Test
    void trainingTypeDao_findByName_returnsEmpty_forUnknownName() {
        rollingBack(() -> assertTrue(trainingTypeDao.findByName("Karate").isEmpty()));
    }

    @Test
    void trainingTypeDao_findAll_returnsSeededTypesInIdOrder() {
        rollingBack(() -> {
            List<String> names = trainingTypeDao.findAll().stream().map(TrainingType::getTrainingTypeName).toList();

            assertEquals(List.of("Fitness", "Yoga", "Zumba", "Stretching", "Resistance"), names);
        });
    }

    // ---------- TraineeDao / UserDao ----------

    @Test
    void traineeDao_save_assignsIdsToTraineeAndUser() {
        rollingBack(() -> {
            Trainee trainee = saveTrainee("John", "Doe", "John.Doe");

            assertNotNull(trainee.getId());
            assertNotNull(trainee.getUser().getId());
        });
    }

    @Test
    void traineeDao_findByUsername_returnsSavedTrainee() {
        rollingBack(() -> {
            saveTrainee("John", "Doe", "John.Doe");

            Optional<Trainee> found = traineeDao.findByUsername("John.Doe");

            assertTrue(found.isPresent());
            assertEquals("Tashkent", found.get().getAddress());
        });
    }

    @Test
    void traineeDao_findByUsername_returnsEmpty_forUnknownUsername() {
        rollingBack(() -> assertTrue(traineeDao.findByUsername("ghost").isEmpty()));
    }

    @Test
    void traineeDao_findByUsername_doesNotReturnTrainers() {
        rollingBack(() -> {
            saveTrainer("Anna", "Lee", "Anna.Lee", "Yoga");

            assertTrue(traineeDao.findByUsername("Anna.Lee").isEmpty());
        });
    }

    @Test
    void traineeDao_update_persistsChanges() {
        rollingBack(() -> {
            Trainee trainee = saveTrainee("John", "Doe", "John.Doe");
            trainee.setAddress("Samarkand");

            Trainee merged = traineeDao.update(trainee);

            assertEquals("Samarkand", merged.getAddress());
            assertEquals("Samarkand", traineeDao.findByUsername("John.Doe").orElseThrow().getAddress());
        });
    }

    @Test
    void traineeDao_delete_removesTraineeAndItsUser() {
        rollingBack(() -> {
            Trainee trainee = saveTrainee("John", "Doe", "John.Doe");

            traineeDao.delete(trainee);

            assertTrue(traineeDao.findByUsername("John.Doe").isEmpty());
            assertTrue(userDao.findByUsername("John.Doe").isEmpty());
        });
    }

    @Test
    void userDao_findByUsername_findsUserOfAnyProfileType() {
        rollingBack(() -> {
            saveTrainee("John", "Doe", "John.Doe");
            saveTrainer("Anna", "Lee", "Anna.Lee", "Yoga");

            assertTrue(userDao.findByUsername("John.Doe").isPresent());
            assertTrue(userDao.findByUsername("Anna.Lee").isPresent());
            assertTrue(userDao.findByUsername("ghost").isEmpty());
        });
    }

    @Test
    void userDao_update_persistsNewPasswordAndActiveFlag() {
        rollingBack(() -> {
            User user = saveTrainee("John", "Doe", "John.Doe").getUser();
            user.setPassword("changedPass");
            user.setActive(false);

            userDao.update(user);

            User reloaded = userDao.findByUsername("John.Doe").orElseThrow();
            assertEquals("changedPass", reloaded.getPassword());
            assertFalse(reloaded.isActive());
        });
    }

    @Test
    void userDao_findUsernamesStartingWith_returnsOnlyMatchingUsernames() {
        rollingBack(() -> {
            saveTrainee("John", "Doe", "John.Doe");
            saveTrainee("John", "Doe", "John.Doe1");
            saveTrainee("Jane", "Roe", "Jane.Roe");

            List<String> result = userDao.findUsernamesStartingWith("John.Doe");

            assertEquals(Set.of("John.Doe", "John.Doe1"), Set.copyOf(result));
            assertTrue(userDao.findUsernamesStartingWith("Nobody").isEmpty());
        });
    }

    // ---------- TrainerDao ----------

    @Test
    void trainerDao_save_andFindByUsername_returnTrainerWithSpecialization() {
        rollingBack(() -> {
            saveTrainer("Anna", "Lee", "Anna.Lee", "Yoga");

            Trainer found = trainerDao.findByUsername("Anna.Lee").orElseThrow();

            assertEquals("Yoga", found.getSpecialization().getTrainingTypeName());
            assertTrue(trainerDao.findByUsername("ghost").isEmpty());
        });
    }

    @Test
    void trainerDao_update_persistsNewSpecialization() {
        rollingBack(() -> {
            Trainer trainer = saveTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
            trainer.setSpecialization(trainingTypeDao.findByName("Fitness").orElseThrow());

            trainerDao.update(trainer);

            assertEquals("Fitness",
                    trainerDao.findByUsername("Anna.Lee").orElseThrow().getSpecialization().getTrainingTypeName());
        });
    }

    @Test
    void trainerDao_findAllByUsernames_returnsOnlyRequestedTrainers() {
        rollingBack(() -> {
            saveTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
            saveTrainer("Mark", "King", "Mark.King", "Fitness");
            saveTrainer("Zed", "Zulu", "Zed.Zulu", "Zumba");

            List<Trainer> result = trainerDao.findAllByUsernames(List.of("Anna.Lee", "Mark.King", "No.Such"));

            assertEquals(Set.of("Anna.Lee", "Mark.King"),
                    result.stream().map(t -> t.getUser().getUsername()).collect(java.util.stream.Collectors.toSet()));
        });
    }

    @Test
    void trainerDao_findAllByUsernames_returnsEmptyList_forNullOrEmptyInput() {
        rollingBack(() -> {
            saveTrainer("Anna", "Lee", "Anna.Lee", "Yoga");

            assertTrue(trainerDao.findAllByUsernames(null).isEmpty());
            assertTrue(trainerDao.findAllByUsernames(List.of()).isEmpty());
        });
    }

    @Test
    void trainerDao_findNotAssignedToTrainee_excludesAssignedTrainers() {
        rollingBack(() -> {
            Trainer anna = saveTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
            saveTrainer("Mark", "King", "Mark.King", "Fitness");
            Trainee john = saveTrainee("John", "Doe", "John.Doe");
            john.getTrainers().add(anna);
            traineeDao.update(john);

            List<Trainer> result = trainerDao.findNotAssignedToTrainee("John.Doe");

            assertEquals(List.of("Mark.King"), result.stream().map(t -> t.getUser().getUsername()).toList());
        });
    }

    @Test
    void trainerDao_findNotAssignedToTrainee_returnsAllTrainers_whenTraineeHasNoTrainers() {
        rollingBack(() -> {
            saveTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
            saveTrainer("Mark", "King", "Mark.King", "Fitness");
            saveTrainee("John", "Doe", "John.Doe");

            assertEquals(2, trainerDao.findNotAssignedToTrainee("John.Doe").size());
        });
    }

    // ---------- TrainingDao ----------

    @Test
    void trainingDao_save_assignsId() {
        rollingBack(() -> {
            Trainee john = saveTrainee("John", "Doe", "John.Doe");
            Trainer anna = saveTrainer("Anna", "Lee", "Anna.Lee", "Yoga");

            Training training = saveTraining(john, anna, "Morning Yoga", "Yoga", LocalDate.of(2026, 9, 1));

            assertNotNull(training.getId());
        });
    }

    @Test
    void trainingDao_findByTraineeUsername_returnsOnlyThatTraineesTrainings_orderedByDate() {
        rollingBack(() -> {
            Trainee john = saveTrainee("John", "Doe", "John.Doe");
            Trainee jane = saveTrainee("Jane", "Roe", "Jane.Roe");
            Trainer anna = saveTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
            saveTraining(john, anna, "Later", "Yoga", LocalDate.of(2026, 9, 20));
            saveTraining(john, anna, "Earlier", "Yoga", LocalDate.of(2026, 9, 1));
            saveTraining(jane, anna, "Not John's", "Yoga", LocalDate.of(2026, 9, 5));

            List<Training> result = trainingDao.findByTraineeUsername("John.Doe", null, null, null, null);

            assertEquals(List.of("Earlier", "Later"), names(result));
        });
    }

    @Test
    void trainingDao_findByTraineeUsername_filtersByDateRange_inclusive() {
        rollingBack(() -> {
            Trainee john = saveTrainee("John", "Doe", "John.Doe");
            Trainer anna = saveTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
            saveTraining(john, anna, "Sep 1", "Yoga", LocalDate.of(2026, 9, 1));
            saveTraining(john, anna, "Sep 15", "Yoga", LocalDate.of(2026, 9, 15));
            saveTraining(john, anna, "Sep 30", "Yoga", LocalDate.of(2026, 9, 30));

            assertEquals(List.of("Sep 15", "Sep 30"), names(trainingDao.findByTraineeUsername(
                    "John.Doe", LocalDate.of(2026, 9, 15), null, null, null)));
            assertEquals(List.of("Sep 1", "Sep 15"), names(trainingDao.findByTraineeUsername(
                    "John.Doe", null, LocalDate.of(2026, 9, 15), null, null)));
            assertEquals(List.of("Sep 15"), names(trainingDao.findByTraineeUsername(
                    "John.Doe", LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 15), null, null)));
        });
    }

    @Test
    void trainingDao_findByTraineeUsername_filtersByTrainerName_caseInsensitiveAndPartial() {
        rollingBack(() -> {
            Trainee john = saveTrainee("John", "Doe", "John.Doe");
            Trainer anna = saveTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
            Trainer mark = saveTrainer("Mark", "King", "Mark.King", "Fitness");
            saveTraining(john, anna, "With Anna", "Yoga", LocalDate.of(2026, 9, 1));
            saveTraining(john, mark, "With Mark", "Fitness", LocalDate.of(2026, 9, 2));

            assertEquals(List.of("With Anna"),
                    names(trainingDao.findByTraineeUsername("John.Doe", null, null, "ANNA", null)));
            assertEquals(List.of("With Mark"),
                    names(trainingDao.findByTraineeUsername("John.Doe", null, null, " mark k", null)));
        });
    }

    @Test
    void trainingDao_findByTraineeUsername_filtersByTrainingType() {
        rollingBack(() -> {
            Trainee john = saveTrainee("John", "Doe", "John.Doe");
            Trainer anna = saveTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
            saveTraining(john, anna, "Yoga session", "Yoga", LocalDate.of(2026, 9, 1));
            saveTraining(john, anna, "Fitness session", "Fitness", LocalDate.of(2026, 9, 2));

            assertEquals(List.of("Fitness session"),
                    names(trainingDao.findByTraineeUsername("John.Doe", null, null, null, "Fitness")));
        });
    }

    @Test
    void trainingDao_findByTraineeUsername_ignoresBlankFilters() {
        rollingBack(() -> {
            Trainee john = saveTrainee("John", "Doe", "John.Doe");
            Trainer anna = saveTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
            saveTraining(john, anna, "Only one", "Yoga", LocalDate.of(2026, 9, 1));

            assertEquals(1, trainingDao.findByTraineeUsername("John.Doe", null, null, "  ", "").size());
        });
    }

    @Test
    void trainingDao_findByTrainerUsername_returnsOnlyThatTrainersTrainings_andFilters() {
        rollingBack(() -> {
            Trainee john = saveTrainee("John", "Doe", "John.Doe");
            Trainee jane = saveTrainee("Jane", "Roe", "Jane.Roe");
            Trainer anna = saveTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
            Trainer mark = saveTrainer("Mark", "King", "Mark.King", "Fitness");
            saveTraining(john, anna, "John-Sep1", "Yoga", LocalDate.of(2026, 9, 1));
            saveTraining(jane, anna, "Jane-Sep20", "Yoga", LocalDate.of(2026, 9, 20));
            saveTraining(john, mark, "Marks", "Fitness", LocalDate.of(2026, 9, 2));

            assertEquals(List.of("John-Sep1", "Jane-Sep20"),
                    names(trainingDao.findByTrainerUsername("Anna.Lee", null, null, null)));
            assertEquals(List.of("Jane-Sep20"),
                    names(trainingDao.findByTrainerUsername("Anna.Lee", LocalDate.of(2026, 9, 10), null, null)));
            assertEquals(List.of("John-Sep1"),
                    names(trainingDao.findByTrainerUsername("Anna.Lee", null, null, "JOHN")));
        });
    }
}