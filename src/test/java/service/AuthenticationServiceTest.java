package service;

import com.epam.training.gym.dao.TraineeDao;
import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.dao.UserDao;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.User;
import com.epam.training.gym.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {
    private UserDao users; private TraineeDao trainees; private TrainerDao trainers; private AuthenticationService service;
    @BeforeEach void setup(){ users=mock(UserDao.class); trainees=mock(TraineeDao.class); trainers=mock(TrainerDao.class); service=new AuthenticationService(); service.setUserDao(users); service.setTraineeDao(trainees); service.setTrainerDao(trainers); }
    @Test void authenticatesValidUser() {
        User user=User.builder().username("Jane.Doe").password("pw").build(); when(users.findByUsername("Jane.Doe")).thenReturn(Optional.of(user));
        assertSame(user,service.authenticate("Jane.Doe","pw"));
    }
    @Test void rejectsWrongPassword() {
        when(users.findByUsername("Jane.Doe")).thenReturn(Optional.of(User.builder().username("Jane.Doe").password("correct").build()));
        assertThrows(AuthenticationException.class,()->service.authenticate("Jane.Doe","wrong"));
    }
    @Test void rejectsBlankCredentials() { assertThrows(AuthenticationException.class,()->service.authenticate(" ","pw")); }
    @Test void traineeCredentialsMatchCorrectPassword() {
        Trainee t=Trainee.builder().user(User.builder().username("Jane.Doe").password("pw").build()).build();
        when(trainees.findByUsername("Jane.Doe")).thenReturn(Optional.of(t));
        assertTrue(service.traineeCredentialsMatch("Jane.Doe","pw")); assertFalse(service.traineeCredentialsMatch("Jane.Doe","bad"));
    }
}
