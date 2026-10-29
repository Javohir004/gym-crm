package health;

import com.epam.training.gym.health.DatabaseResponseTimeHealthIndicator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseResponseTimeHealthIndicatorTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection connection;

    @BeforeEach
    void setUp() {
    }

    @Test
    void health_isUp_whenConnectionIsValidAndFast() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        Health health = new DatabaseResponseTimeHealthIndicator(dataSource, 10_000).health();

        assertEquals(Status.UP, health.getStatus());
        assertTrue(health.getDetails().containsKey("responseTimeMs"));
    }

    @Test
    void health_isOutOfService_whenDatabaseIsSlow() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        // threshold -1 ms: any measured time is "slow"
        Health health = new DatabaseResponseTimeHealthIndicator(dataSource, -1).health();

        assertEquals(Status.OUT_OF_SERVICE, health.getStatus());
    }

    @Test
    void health_isDown_whenConnectionIsNotValid() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(false);

        Health health = new DatabaseResponseTimeHealthIndicator(dataSource, 10_000).health();

        assertEquals(Status.DOWN, health.getStatus());
    }

    @Test
    void health_isDown_whenConnectionCannotBeOpened() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("no connection"));

        Health health = new DatabaseResponseTimeHealthIndicator(dataSource, 10_000).health();

        assertEquals(Status.DOWN, health.getStatus());
    }
}