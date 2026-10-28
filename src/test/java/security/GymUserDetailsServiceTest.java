package security;

import com.epam.training.gym.dao.UserDao;
import com.epam.training.gym.model.User;
import com.epam.training.gym.security.GymUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GymUserDetailsServiceTest {

    @Mock
    private UserDao userDao;

    @Test
    void loadUserByUsername_exposesUsernameAndStoredHash() {
        User user = User.builder().username("John.Doe").password("$2a$10$hash").active(true).build();
        when(userDao.findByUsername("John.Doe")).thenReturn(Optional.of(user));

        UserDetails details = new GymUserDetailsService(userDao).loadUserByUsername("John.Doe");

        assertEquals("John.Doe", details.getUsername());
        assertEquals("$2a$10$hash", details.getPassword());
        assertTrue(details.isEnabled());
    }

    @Test
    void loadUserByUsername_throws_whenUserDoesNotExist() {
        when(userDao.findByUsername("No.Body")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> new GymUserDetailsService(userDao).loadUserByUsername("No.Body"));
    }
}