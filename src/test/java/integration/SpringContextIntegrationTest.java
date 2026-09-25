package integration;

import com.epam.training.gym.config.AppConfig;
import com.epam.training.gym.facade.GymFacade;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import com.epam.training.gym.model.TrainingType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SpringContextIntegrationTest {

    private static AnnotationConfigApplicationContext context;
    private static GymFacade facade;

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

    @Test
    @Order(1)
    void contextStartsAndSeedsStorageFromCsvFiles() {
        assertEquals(2, facade.selectAllTrainees().size());
        assertEquals(2, facade.selectAllTrainers().size());
        assertEquals(2, facade.selectAllTrainings().size());
    }

    @Test
    @Order(2)
    void createTrainee_generatesSuffixedUsername_whenNameAlreadySeeded() {
        // trainee-data.csv already contains John.Doe
        Trainee trainee = facade.createTrainee(Trainee.builder()
                .firstName("John")
                .lastName("Doe")
                .build());

        assertEquals("John.Doe1", trainee.getUsername());
        assertEquals(10, trainee.getPassword().length());
        assertTrue(trainee.isActive());


        facade.deleteTrainee(trainee.getUserId());
    }

    @Test
    @Order(3)
    void createTrainer_generatesSuffixedUsername_whenNameAlreadySeeded() {

        Trainer trainer = facade.createTrainer(Trainer.builder()
                .firstName("Anna")
                .lastName("Lee")
                .specialization(new TrainingType(1L, "Yoga"))
                .build());

        assertEquals("Anna.Lee1", trainer.getUsername());
        assertEquals(10, trainer.getPassword().length());
    }

    @Test
    @Order(4)
    void fullLifecycle_createUpdateDeleteTrainee() {
        Trainee created = facade.createTrainee(Trainee.builder()
                .firstName("Temp")
                .lastName("User")
                .build());
        assertTrue(facade.selectTrainee(created.getUserId()).isPresent());

        created.setAddress("Updated address");
        facade.updateTrainee(created);
        assertEquals("Updated address",
                facade.selectTrainee(created.getUserId()).get().getAddress());

        facade.deleteTrainee(created.getUserId());
        assertFalse(facade.selectTrainee(created.getUserId()).isPresent());
    }

    @Test
    @Order(5)
    void createTraining_linksToExistingSeededTraineeAndTrainer() {
        Trainee trainee = facade.selectAllTrainees().get(0);
        Trainer trainer = facade.selectAllTrainers().get(0);

        Training training = facade.createTraining(Training.builder()
                .traineeId(trainee.getUserId())
                .trainerId(trainer.getUserId())
                .trainingName("Extra session")
                .trainingType(new TrainingType(1L, "Yoga"))
                .trainingDate(java.time.LocalDate.now())
                .trainingDuration(30)
                .build());

        assertTrue(facade.selectTraining(training.getTrainingId()).isPresent());
    }
}