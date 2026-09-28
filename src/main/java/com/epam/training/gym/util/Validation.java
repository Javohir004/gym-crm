package com.epam.training.gym.util;

import com.epam.training.gym.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Validation {

    private static final Logger log = LoggerFactory.getLogger(Validation.class);

    private Validation() {
    }

    public static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            log.warn("Validation failed: {} must not be empty", fieldName);
            throw new ValidationException(fieldName + " must not be empty");
        }
    }

    public static void requireNotNull(Object value, String fieldName) {
        if (value == null) {
            log.warn("Validation failed: {} is required", fieldName);
            throw new ValidationException(fieldName + " is required");
        }
    }

    public static void requirePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            log.warn("Validation failed: {} must be a positive number, got {}", fieldName, value);
            throw new ValidationException(fieldName + " must be a positive number");
        }
    }
}