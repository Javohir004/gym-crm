package com.epam.training.gym.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error details")
public record ErrorResponse(
        @Schema(example = "Invalid username or password") String message,
        @Schema(example = "401") int status,
        @Schema(description = "Same id you can find in the server logs for this request")
        String transactionId) {
}