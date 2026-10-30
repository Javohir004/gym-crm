package com.epam.training.gym.service;

import com.epam.training.gym.dao.UserDao;
import com.epam.training.gym.exception.ConflictException;
import com.epam.training.gym.model.User;
import com.epam.training.gym.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class UserAccountService {

    private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);

    private UserDao userDao;
    private UsernameGenerator usernameGenerator;
    private PasswordGenerator passwordGenerator;
    private PasswordEncoder passwordEncoder;
    private AuthenticationService authenticationService;

    /** A new, not yet saved user (password stored as a hash) plus the plain password to hand out once. */
    public record NewAccount(User user, String rawPassword) {

        @Override
        public String toString() {
            return "NewAccount[username=" + user.getUsername() + "]";
        }
    }

    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    @Autowired
    public void setUsernameGenerator(UsernameGenerator usernameGenerator) {
        this.usernameGenerator = usernameGenerator;
    }

    @Autowired
    public void setPasswordGenerator(PasswordGenerator passwordGenerator) {
        this.passwordGenerator = passwordGenerator;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public NewAccount newUser(String firstName, String lastName) {
        Validation.requireText(firstName, "First name");
        Validation.requireText(lastName, "Last name");

        String rawPassword = passwordGenerator.generate();
        User user = User.builder()
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .username(usernameGenerator.generate(firstName, lastName))
                .password(passwordEncoder.encode(rawPassword))
                .active(true)
                .build();
        return new NewAccount(user, rawPassword);
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = authenticationService.authenticate(username, oldPassword);
        changePassword(user, newPassword);
    }

    public void changePassword(User user, String newPassword) {
        Validation.requireText(newPassword, "New password");
        user.setPassword(passwordEncoder.encode(newPassword));
        userDao.update(user);
        log.info("Password changed for username={}", user.getUsername());
    }

    public void setActive(User user, boolean active) {
        if (user.isActive() == active) {
            throw new ConflictException("User '" + user.getUsername() + "' is already "
                    + (active ? "active" : "inactive"));
        }
        user.setActive(active);
        userDao.update(user);
        log.info("User username={} is now {}", user.getUsername(), active ? "active" : "inactive");
    }

    public boolean toggleActive(User user) {
        user.setActive(!user.isActive());
        userDao.update(user);
        log.info("User username={} is now {}", user.getUsername(), user.isActive() ? "active" : "inactive");
        return user.isActive();
    }

}