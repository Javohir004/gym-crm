package util;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.exception.ValidationException;
import com.epam.training.gym.util.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import support.LogCapture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    void requireText_throws_whenValueIsNullOrBlank(String value) {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> Validation.requireText(value, "First name"));

        assertEquals("First name must not be empty", ex.getMessage());
    }

    @Test
    void requireText_passes_whenValueHasText() {
        assertDoesNotThrow(() -> Validation.requireText("John", "First name"));
    }

    @Test
    void requireNotNull_throws_whenValueIsNull() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> Validation.requireNotNull(null, "Training date"));

        assertEquals("Training date is required", ex.getMessage());
    }

    @Test
    void requireNotNull_passes_whenValueIsPresent() {
        assertDoesNotThrow(() -> Validation.requireNotNull(new Object(), "Training date"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    void requirePositive_throws_whenValueIsZeroOrNegative(int value) {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> Validation.requirePositive(value, "Training duration"));

        assertEquals("Training duration must be a positive number", ex.getMessage());
    }

    @Test
    void requirePositive_throws_whenValueIsNull() {
        assertThrows(ValidationException.class, () -> Validation.requirePositive(null, "Training duration"));
    }

    @Test
    void requirePositive_passes_whenValueIsPositive() {
        assertDoesNotThrow(() -> Validation.requirePositive(1, "Training duration"));
    }

    @Test
    void failedValidation_isLoggedAsWarning() {
        try (LogCapture logs = new LogCapture(Validation.class)) {
            assertThrows(ValidationException.class, () -> Validation.requireText(" ", "Last name"));

            assertTrue(logs.messages(Level.WARN).stream().anyMatch(m -> m.contains("Last name")));
        }
    }

    @Test
    void successfulValidation_logsNothing() {
        try (LogCapture logs = new LogCapture(Validation.class)) {
            Validation.requireText("ok", "Field");

            assertTrue(logs.allMessages().isEmpty());
        }
    }
}