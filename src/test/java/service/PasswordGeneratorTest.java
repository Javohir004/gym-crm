package service;

import com.epam.training.gym.service.PasswordGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordGeneratorTest {
    @Test void generatedPasswordHasExpectedLengthAndAllowedCharacters() {
        String password = new PasswordGenerator().generate();
        assertEquals(10, password.length());
        assertTrue(password.matches("[a-zA-Z0-9]{10}"));
    }
    @Test void consecutivePasswordsAreNotConstant() {
        PasswordGenerator generator = new PasswordGenerator();
        boolean differs = false;
        String first = generator.generate();
        for (int i = 0; i < 10; i++) differs |= !first.equals(generator.generate());
        assertTrue(differs);
    }
}
