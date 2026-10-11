package com.epam.training.gym.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fields needed to change a password")
public record ChangeLoginRequest(

        @Schema(description = "Username", requiredMode = Schema.RequiredMode.REQUIRED, example = "John.Doe")
        String username,

        @Schema(description = "Current password", requiredMode = Schema.RequiredMode.REQUIRED)
        String oldPassword,

        @Schema(description = "New password", requiredMode = Schema.RequiredMode.REQUIRED)
        String newPassword) {
}