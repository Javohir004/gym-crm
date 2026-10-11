package integration;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AppConfig.class)
class DaoIntegrationTest {

    @Autowired
    private TraineeDao traineeDao;
    @Autowired
    private TrainerDao trainerDao;
    @Autowired
    private TrainingDao trainingDao;
    @Autowired
    private UserDao userDao;
    @Autowired
    private TrainingTypeDao trainingTypeDao;

    private static String unique(String base) {
        return base + "." + UUID.randomUUID().toString().substring(0, 8);
    }

    private User newUser(String username) {
        return User.builder().firstName("Test").lastName("User")
                .username(username).password("pass123456").active(true).build();
    }


    // ---------- TrainingTypeDao (read-only, seeded by import.sql) ----------

    @Test
    @Transactional
    void trainingTypeDao_findAll_returnsTheSeededConstantList() {
        List<TrainingType> types = trainingTypeDao.findAll();

        assertTrue(types.size() >= 5);
        assertTrue(types.stream().anyMatch(t -> t.getTrainingTypeName().equals("Yoga")));
    }

    @Test
    @Transactional
    void trainingTypeDao_findByName_findsExistingType() {
        Optional<TrainingType> found = trainingTypeDao.findByName("Fitness");

        assertTrue(found.isPresent());
    }

    @Test
    @Transactional
    void trainingTypeDao_findByName_isEmpty_forUnknownType() {
        assertTrue(trainingTypeDao.findByName("No Such Type").isEmpty());
    }


    // ---------- UserDao ----------

    @Test
    @Transactional
    void userDao_findByUsername_findsAfterTraineeIsSaved() {
        String username = unique("Test.Trainee");
        Trainee trainee = Trainee.builder().user(newUser(username)).build();
        traineeDao.save(trainee);

        Optional<User> found = userDao.findByUsername(username);

        assertTrue(found.isPresent());
        assertEquals(username, found.get().getUsername());
    }

    @Test
    @Transactional
    void userDao_findUsernamesStartingWith_findsAllVariantsOfTheSameName() {
        String base = unique("Zed.Zed");
        traineeDao.save(Trainee.builder().user(newUser(base)).build());
        traineeDao.save(Trainee.builder().user(newUser(base + "1")).build());

        List<String> found = userDao.findUsernamesStartingWith(base);

        assertTrue(found.contains(base));
        assertTrue(found.contains(base + "1"));
    }

    @Test
    @Transactional
    void userDao_update_persistsChangedPassword() {
        String username = unique("Test.Trainee");
        Trainee trainee = Trainee.builder().user(newUser(username)).build();
        traineeDao.save(trainee);

        trainee.getUser().setPassword("brandNewPassword1");
        userDao.update(trainee.getUser());

        User reloaded = userDao.findByUsername(username).orElseThrow();
        assertEquals("brandNewPassword1", reloaded.getPassword());
    }


    // ---------- TraineeDao ----------

    @Test
    @Transactional
    void traineeDao_save_thenFindByUsername_returnsTheSameTrainee() {
        String username = unique("Test.Trainee");
        Trainee trainee = Trainee.builder().user(newUser(username))
                .dateOfBirth(LocalDate.of(2000, 1, 15)).address("Tashkent").build();

        traineeDao.save(trainee);

        Trainee found = traineeDao.findByUsername(username).orElseThrow();
        assertEquals("Tashkent", found.getAddress());
        assertEquals(LocalDate.of(2000, 1, 15), found.getDateOfBirth());
    }

    @Test
    @Transactional
    void traineeDao_findByUsername_isEmpty_forUnknownUsername() {
        assertTrue(traineeDao.findByUsername(unique("No.Such")).isEmpty());
    }

    @Test
    @Transactional
    void traineeDao_update_persistsAddressChange() {
        String username = unique("Test.Trainee");
        Trainee trainee = Trainee.builder().user(newUser(username)).address("Old Address").build();
        traineeDao.save(trainee);

        trainee.setAddress("New Address");
        traineeDao.update(trainee);

        Trainee reloaded = traineeDao.findByUsername(username).orElseThrow();
        assertEquals("New Address", reloaded.getAddress());
    }

    @Test
    @Transactional
    void traineeDao_delete_removesTheTraineeAndItsUser() {
        String username = unique("Test.Trainee");
        Trainee trainee = Trainee.builder().user(newUser(username)).build();
        traineeDao.save(trainee);

        traineeDao.delete(trainee);

        assertTrue(traineeDao.findByUsername(username).isEmpty());
        assertTrue(userDao.findByUsername(username).isEmpty());
    }


    // ---------- TrainerDao ----------

    @Test
    @Transactional
    void trainerDao_save_thenFindByUsername_returnsTheSameTrainer() {
        TrainingType yoga = trainingTypeDao.findByName("Yoga").orElseThrow();
        String username = unique("Test.Trainer");
        Trainer trainer = Trainer.builder().user(newUser(username)).specialization(yoga).build();

        trainerDao.save(trainer);

        Trainer found = trainerDao.findByUsername(username).orElseThrow();
        assertEquals("Yoga", found.getSpecialization().getTrainingTypeName());
    }

    @Test
    @Transactional
    void trainerDao_update_persistsSpecializationChange() {
        TrainingType yoga = trainingTypeDao.findByName("Yoga").orElseThrow();
        TrainingType fitness = trainingTypeDao.findByName("Fitness").orElseThrow();
        String username = unique("Test.Trainer");
        Trainer trainer = Trainer.builder().user(newUser(username)).specialization(yoga).build();
        trainerDao.save(trainer);

        trainer.setSpecialization(fitness);
        trainerDao.update(trainer);

        Trainer reloaded = trainerDao.findByUsername(username).orElseThrow();
        assertEquals("Fitness", reloaded.getSpecialization().getTrainingTypeName());
    }

    @Test
    @Transactional
    void trainerDao_findAllByUsernames_returnsOnlyRequestedOnes() {
        TrainingType yoga = trainingTypeDao.findByName("Yoga").orElseThrow();
        String username = unique("Test.Trainer");
        trainerDao.save(Trainer.builder().user(newUser(username)).specialization(yoga).build());

        List<Trainer> found = trainerDao.findAllByUsernames(Set.of(username));

        assertEquals(1, found.size());
        assertEquals(username, found.get(0).getUser().getUsername());
    }

    @Test
    @Transactional
    void trainerDao_findAllByUsernames_returnsEmptyList_whenGivenNoUsernames() {
        assertTrue(trainerDao.findAllByUsernames(Set.of()).isEmpty());
    }

    @Test
    @Transactional
    void trainerDao_findNotAssignedToTrainee_excludesAnAssignedTrainer_butKeepsOthers() {
        TrainingType yoga = trainingTypeDao.findByName("Yoga").orElseThrow();
        String assignedUsername = unique("Test.Trainer");
        String freeUsername = unique("Test.Trainer");
        Trainer assigned = Trainer.builder().user(newUser(assignedUsername)).specialization(yoga).build();
        Trainer free = Trainer.builder().user(newUser(freeUsername)).specialization(yoga).build();
        trainerDao.save(assigned);
        trainerDao.save(free);

        String traineeUsername = unique("Test.Trainee");
        Trainee trainee = Trainee.builder().user(newUser(traineeUsername)).trainers(Set.of(assigned)).build();
        traineeDao.save(trainee);

        List<Trainer> notAssigned = trainerDao.findNotAssignedToTrainee(traineeUsername);

        assertTrue(notAssigned.stream().anyMatch(t -> t.getUser().getUsername().equals(freeUsername)));
        assertFalse(notAssigned.stream().anyMatch(t -> t.getUser().getUsername().equals(assignedUsername)));
    }

    @Test
    @Transactional
    void trainerDao_findNotAssignedToTraineeActive_excludesInactiveTrainers() {
        TrainingType yoga = trainingTypeDao.findByName("Yoga").orElseThrow();
        String activeUsername = unique("Test.Trainer");
        String inactiveUsername = unique("Test.Trainer");
        User inactiveUser = newUser(inactiveUsername);
        inactiveUser.setActive(false);
        trainerDao.save(Trainer.builder().user(newUser(activeUsername)).specialization(yoga).build());
        trainerDao.save(Trainer.builder().user(inactiveUser).specialization(yoga).build());

        String traineeUsername = unique("Test.Trainee");
        traineeDao.save(Trainee.builder().user(newUser(traineeUsername)).build());

        List<Trainer> result = trainerDao.findNotAssignedToTraineeActive(traineeUsername);

        assertTrue(result.stream().anyMatch(t -> t.getUser().getUsername().equals(activeUsername)));
        assertFalse(result.stream().anyMatch(t -> t.getUser().getUsername().equals(inactiveUsername)));
    }


    // ---------- TrainingDao ----------

    @Test
    @Transactional
    void trainingDao_save_thenFindByTraineeUsername_returnsIt() {
        TrainingType yoga = trainingTypeDao.findByName("Yoga").orElseThrow();
        String traineeUsername = unique("Test.Trainee");
        String trainerUsername = unique("Test.Trainer");
        Trainee trainee = Trainee.builder().user(newUser(traineeUsername)).build();
        Trainer trainer = Trainer.builder().user(newUser(trainerUsername)).specialization(yoga).build();
        traineeDao.save(trainee);
        trainerDao.save(trainer);

        Training training = Training.builder().trainee(trainee).trainer(trainer)
                .trainingName("Morning Yoga").trainingType(yoga)
                .trainingDate(LocalDate.of(2026, 9, 1)).trainingDuration(60).build();
        trainingDao.save(training);

        List<Training> found = trainingDao.findByTraineeUsername(traineeUsername, null, null, null, null);

        assertEquals(1, found.size());
        assertEquals("Morning Yoga", found.get(0).getTrainingName());
    }

    @Test
    @Transactional
    void trainingDao_findByTraineeUsername_filtersByDateRangeAndType() {
        TrainingType yoga = trainingTypeDao.findByName("Yoga").orElseThrow();
        TrainingType fitness = trainingTypeDao.findByName("Fitness").orElseThrow();
        String traineeUsername = unique("Test.Trainee");
        String trainerUsername = unique("Test.Trainer");
        Trainee trainee = Trainee.builder().user(newUser(traineeUsername)).build();
        Trainer trainer = Trainer.builder().user(newUser(trainerUsername)).specialization(yoga).build();
        traineeDao.save(trainee);
        trainerDao.save(trainer);

        trainingDao.save(Training.builder().trainee(trainee).trainer(trainer)
                .trainingName("Early Yoga").trainingType(yoga)
                .trainingDate(LocalDate.of(2026, 1, 1)).trainingDuration(60).build());
        trainingDao.save(Training.builder().trainee(trainee).trainer(trainer)
                .trainingName("September Fitness").trainingType(fitness)
                .trainingDate(LocalDate.of(2026, 9, 10)).trainingDuration(45).build());

        List<Training> result = trainingDao.findByTraineeUsername(traineeUsername,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null, "Fitness");

        assertEquals(1, result.size());
        assertEquals("September Fitness", result.get(0).getTrainingName());
    }

    @Test
    @Transactional
    void trainingDao_findByTrainerUsername_filtersByTraineeName() {
        TrainingType yoga = trainingTypeDao.findByName("Yoga").orElseThrow();
        String trainerUsername = unique("Test.Trainer");
        String traineeUsername = unique("Test.Trainee");
        User traineeUser = User.builder().firstName("Special").lastName("Person")
                .username(traineeUsername).password("pass123456").active(true).build();
        Trainee trainee = Trainee.builder().user(traineeUser).build();
        Trainer trainer = Trainer.builder().user(newUser(trainerUsername)).specialization(yoga).build();
        traineeDao.save(trainee);
        trainerDao.save(trainer);

        trainingDao.save(Training.builder().trainee(trainee).trainer(trainer)
                .trainingName("Yoga Session").trainingType(yoga)
                .trainingDate(LocalDate.of(2026, 9, 1)).trainingDuration(60).build());

        List<Training> found = trainingDao.findByTrainerUsername(trainerUsername, null, null, "special");
        List<Training> notFound = trainingDao.findByTrainerUsername(trainerUsername, null, null, "nobody");

        assertEquals(1, found.size());
        assertTrue(notFound.isEmpty());
    }
}