package com.epam.training.gym.logging;

import java.util.regex.Pattern;

public final class SensitiveDataMasker {

    static final String MASK = "***";

    private static final Pattern SENSITIVE_JSON_FIELD = Pattern.compile(
            "(\"(?:password|oldPassword|newPassword|token)\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"",
            Pattern.CASE_INSENSITIVE);

    private SensitiveDataMasker() {
    }

    public static String mask(String json) {
        return SENSITIVE_JSON_FIELD.matcher(json).replaceAll("$1\"" + MASK + "\"");
    }
}
