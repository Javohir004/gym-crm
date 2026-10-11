package service;

import com.epam.training.gym.dao.UserDao;
import com.epam.training.gym.exception.ValidationException;
import com.epam.training.gym.service.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsernameGeneratorTest {
    private UserDao dao;
    private UsernameGenerator generator;
    @BeforeEach void setup() { dao = mock(UserDao.class); generator = new UsernameGenerator(); generator.setUserDao(dao); }
    @Test void generatesBaseWhenAvailable() {
        when(dao.findUsernamesStartingWith("Jane.Doe")).thenReturn(List.of());
        assertEquals("Jane.Doe", generator.generate(" Jane ", "Doe"));
    }
    @Test void appendsFirstFreeSerialWhenNameTaken() {
        when(dao.findUsernamesStartingWith("Jane.Doe")).thenReturn(List.of("Jane.Doe", "Jane.Doe1", "Jane.Doe3"));
        assertEquals("Jane.Doe2", generator.generate("Jane", "Doe"));
    }
    @Test void rejectsBlankName() { assertThrows(ValidationException.class, () -> generator.generate(" ", "Doe")); }
}
