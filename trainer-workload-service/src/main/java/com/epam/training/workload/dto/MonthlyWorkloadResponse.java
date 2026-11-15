package com.epam.training.workload.dto;

import java.time.Month;

public record MonthlyWorkloadResponse(String trainerUsername, int year, Month month, int trainingSummaryDuration) {
}
