package com.epam.training.gym.controller;


/**
 * URL versioning: every REST endpoint lives under {@link #V1}. A breaking change gets a new
 * prefix (e.g. /api/v2) so existing clients keep working on the old version.
 */
public final class ApiPaths {

    public static final String V1 = "/api/v1";

    private ApiPaths() {
    }
}
