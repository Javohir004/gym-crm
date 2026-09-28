package com.epam.training.gym.service;

import com.epam.training.gym.dao.UserDao;
import com.epam.training.gym.model.User;
import com.epam.training.gym.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class UserAccountService {

    private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);

    private UserDao userDao;
    private UsernameGenerator usernameGenerator;
    private PasswordGenerator passwordGenerator;

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

    public User newUser(String firstName, String lastName) {
        Validation.requireText(firstName, "First name");
        Validation.requireText(lastName, "Last name");

        return User.builder()
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .username(usernameGenerator.generate(firstName, lastName))
                .password(passwordGenerator.generate())
                .active(true)
                .build();
    }

    public void changePassword(User user, String newPassword) {
        Validation.requireText(newPassword, "New password");
        user.setPassword(newPassword);
        userDao.update(user);
        log.info("Password changed for username={}", user.getUsername());
    }


    public boolean toggleActive(User user) {
        user.setActive(!user.isActive());
        userDao.update(user);
        log.info("User username={} is now {}", user.getUsername(), user.isActive() ? "active" : "inactive");
        return user.isActive();
    }
}