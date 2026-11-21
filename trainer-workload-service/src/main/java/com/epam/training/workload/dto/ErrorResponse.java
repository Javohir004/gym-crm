package com.epam.training.workload.dto;

public record ErrorResponse(String message, int status, String transactionId) {
}
