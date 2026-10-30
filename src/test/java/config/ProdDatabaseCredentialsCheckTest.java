package config;

import com.epam.training.gym.config.ProdDatabaseCredentialsCheck;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProdDatabaseCredentialsCheckTest {

    private final ProdDatabaseCredentialsCheck check = new ProdDatabaseCredentialsCheck(new DeferredLogs());
    private final SpringApplication application = new SpringApplication();

    @Test
    void prodWithBothVariables_startsNormally() {
        MockEnvironment environment = prodEnvironment()
                .withProperty("DB_USERNAME", "prod_user")
                .withProperty("DB_PASSWORD", "prod_pass");

        assertDoesNotThrow(() -> check.postProcessEnvironment(environment, application));
    }

    @Test
    void prodWithoutVariables_failsAndNamesBoth() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> check.postProcessEnvironment(prodEnvironment(), application));

        assertTrue(e.getMessage().contains("DB_USERNAME"));
        assertTrue(e.getMessage().contains("DB_PASSWORD"));
    }

    @Test
    void prodWithBlankPassword_fails() {
        MockEnvironment environment = prodEnvironment()
                .withProperty("DB_USERNAME", "prod_user")
                .withProperty("DB_PASSWORD", " ");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> check.postProcessEnvironment(environment, application));

        assertTrue(e.getMessage().contains("DB_PASSWORD"));
    }

    @Test
    void otherProfile_withoutVariables_isNotChecked() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        assertDoesNotThrow(() -> check.postProcessEnvironment(environment, application));
    }

    private static MockEnvironment prodEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment;
    }
}
