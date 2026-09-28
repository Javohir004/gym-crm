package com.epam.training.gym.service;

import com.epam.training.gym.dao.UserDao;
import com.epam.training.gym.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;


@Component
public class UsernameGenerator {

    private static final Logger log = LoggerFactory.getLogger(UsernameGenerator.class);

    private UserDao userDao;

    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public String generate(String firstName, String lastName) {
        Validation.requireText(firstName, "First name");
        Validation.requireText(lastName, "Last name");

        String base = firstName.trim() + "." + lastName.trim();
        Set<String> taken = new HashSet<>(userDao.findUsernamesStartingWith(base));

        if (!taken.contains(base)) {
            log.debug("Generated username '{}'", base);
            return base;
        }

        int serial = 1;
        while (taken.contains(base + serial)) {
            serial++;
        }
        String result = base + serial;
        log.debug("Username '{}' already taken, generated '{}'", base, result);
        return result;
    }
}