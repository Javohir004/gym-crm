package com.epam.training.gym.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;


@Component
public class DatabaseResponseTimeHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseResponseTimeHealthIndicator.class);

    private final DataSource dataSource;
    private final long slowThresholdMs;

    public DatabaseResponseTimeHealthIndicator(
            DataSource dataSource,
            @Value("${gym.health.db-slow-threshold-ms:500}") long slowThresholdMs) {
        this.dataSource = dataSource;
        this.slowThresholdMs = slowThresholdMs;
    }

    @Override
    public Health health() {
        long start = System.nanoTime();
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            if (!valid) {
                log.error("Health check failed: database connection is not valid");
                return Health.down().withDetail("reason", "Connection is not valid").build();
            }
            if (elapsedMs > slowThresholdMs) {
                log.warn("Database is slow: {} ms (threshold {} ms)", elapsedMs, slowThresholdMs);
                return Health.outOfService()
                        .withDetail("responseTimeMs", elapsedMs)
                        .withDetail("thresholdMs", slowThresholdMs)
                        .build();
            }
            return Health.up()
                    .withDetail("responseTimeMs", elapsedMs)
                    .withDetail("thresholdMs", slowThresholdMs)
                    .build();
        } catch (Exception e) {
            log.error("Health check failed: cannot get a database connection", e);
            return Health.down(e).build();
        }
    }
}