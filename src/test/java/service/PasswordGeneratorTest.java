package service;



import com.epam.training.gym.service.PasswordGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordGeneratorTest {

    private final PasswordGenerator generator = new PasswordGenerator();

    @Test
    void generate_returnsStringOfExpectedLength() {
        String password = generator.generate();

        assertEquals(10, password.length());
    }

    @Test
    void generate_returnsOnlyAlphanumericCharacters() {
        String password = generator.generate();

        assertTrue(password.chars().allMatch(Character::isLetterOrDigit));
    }

    @Test
    void generate_producesDifferentValuesAcrossCalls() {
        String first = generator.generate();
        String second = generator.generate();

        assertNotEquals(first, second);
    }
}
