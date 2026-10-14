package health;

import com.epam.training.gym.health.TrainingTypesHealthIndicator;
import com.epam.training.gym.model.TrainingType;
import com.epam.training.gym.service.TrainingTypeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingTypesHealthIndicatorTest {

    @Mock
    private TrainingTypeService trainingTypeService;
    @InjectMocks
    private TrainingTypesHealthIndicator indicator;

    @Test
    void health_isUp_whenTypesExist() {
        when(trainingTypeService.getAll()).thenReturn(List.of(
                TrainingType.builder().trainingTypeName("Yoga").build(),
                TrainingType.builder().trainingTypeName("Fitness").build()));

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(2, health.getDetails().get("trainingTypes"));
    }

    @Test
    void health_isDown_whenListIsEmpty() {
        when(trainingTypeService.getAll()).thenReturn(List.of());

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("reason"));
    }

    @Test
    void health_isDown_whenServiceThrows() {
        when(trainingTypeService.getAll()).thenThrow(new IllegalStateException("db is gone"));

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("error"));
    }
}