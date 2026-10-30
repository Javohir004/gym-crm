package com.epam.training.gym.config;

import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

import java.util.List;


/**
 * Spring keeps an unresolved {@code ${DB_USERNAME}} as literal text instead of failing,
 * so without this check prod would silently start with the placeholder as the db user.
 * Runs as an {@link EnvironmentPostProcessor} (registered in META-INF/spring.factories)
 * so it fails before the DataSource is created.
 */
public class ProdDatabaseCredentialsCheck implements EnvironmentPostProcessor {

    private static final List<String> REQUIRED_VARIABLES = List.of("DB_USERNAME", "DB_PASSWORD");

    private final Log log;

    public ProdDatabaseCredentialsCheck(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(ProdDatabaseCredentialsCheck.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }
        List<String> missing = REQUIRED_VARIABLES.stream()
                .filter(name -> isBlank(environment.getProperty(name)))
                .toList();
        if (!missing.isEmpty()) {
            log.error("Refusing to start in prod: missing environment variable(s) " + missing);
            throw new IllegalStateException("Missing required environment variable(s) for prod: " + missing);
        }
        log.info("Prod database credentials are provided via environment variables");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
