package metrics;

import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.metrics.GymMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GymMetricsTest {

    private SimpleMeterRegistry registry;
    private TrainerDao trainerDao;
    private GymMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        trainerDao = mock(TrainerDao.class);
        metrics = new GymMetrics(registry, trainerDao);
    }

    @Test
    void registrations_areCountedPerRole() {
        metrics.traineeRegistered();
        metrics.traineeRegistered();
        metrics.trainerRegistered();

        assertEquals(2.0, registry.get("gym.registrations").tag("role", "trainee").counter().count());
        assertEquals(1.0, registry.get("gym.registrations").tag("role", "trainer").counter().count());
    }

    @Test
    void trainingsAdded_isCounted() {
        metrics.trainingAdded();

        assertEquals(1.0, registry.get("gym.trainings.added").counter().count());
    }

    @Test
    void authenticationFailures_areCounted() {
        metrics.authenticationFailed();
        metrics.authenticationFailed();
        metrics.authenticationFailed();

        assertEquals(3.0, registry.get("gym.authentication.failures").counter().count());
    }

    @Test
    void activeTrainersGauge_readsTheCurrentValueFromTheDao() {
        when(trainerDao.countActive()).thenReturn(7L);

        assertEquals(7.0, registry.get("gym.trainers.active").gauge().value());
    }
}