package health;

import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.health.TrainerAvailabilityHealthIndicator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerAvailabilityHealthIndicatorTest {

    @Mock
    private TrainerDao trainerDao;
    @InjectMocks
    private TrainerAvailabilityHealthIndicator indicator;

    @Test
    void health_isUp_withCounts() {
        when(trainerDao.countAll()).thenReturn(5L);
        when(trainerDao.countActive()).thenReturn(3L);

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(5L, health.getDetails().get("totalTrainers"));
        assertEquals(3L, health.getDetails().get("activeTrainers"));
        assertEquals(2L, health.getDetails().get("inactiveTrainers"));
    }

    @Test
    void health_isUp_whenThereAreNoTrainersAtAll() {
        when(trainerDao.countAll()).thenReturn(0L);
        when(trainerDao.countActive()).thenReturn(0L);

        assertEquals(Status.UP, indicator.health().getStatus());
    }

    @Test
    void health_isUnknownWithWarning_whenAllTrainersAreInactive() {
        when(trainerDao.countAll()).thenReturn(4L);
        when(trainerDao.countActive()).thenReturn(0L);

        Health health = indicator.health();

        assertEquals(Status.UNKNOWN, health.getStatus());
        assertEquals("All trainers are inactive", health.getDetails().get("warning"));
    }

    @Test
    void health_isDown_whenDaoThrows() {
        when(trainerDao.countAll()).thenThrow(new IllegalStateException("db is gone"));

        assertEquals(Status.DOWN, indicator.health().getStatus());
    }
}