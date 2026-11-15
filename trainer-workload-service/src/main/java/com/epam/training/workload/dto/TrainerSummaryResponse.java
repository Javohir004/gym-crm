package com.epam.training.workload.dto;

import com.epam.training.workload.model.TrainerWorkload;

import java.time.Month;
import java.util.List;

public record TrainerSummaryResponse(
        String trainerUsername,
        String trainerFirstName,
        String trainerLastName,
        boolean trainerStatus,
        List<YearSummary> years) {

    public record YearSummary(int year, List<MonthSummary> months) {
    }

    public record MonthSummary(Month month, int trainingSummaryDuration) {
    }

    public static TrainerSummaryResponse from(TrainerWorkload workload) {
        List<YearSummary> years = workload.getMinutesByYear().entrySet().stream()
                .map(year -> new YearSummary(year.getKey(), year.getValue().entrySet().stream()
                        .map(month -> new MonthSummary(month.getKey(), month.getValue()))
                        .toList()))
                .toList();
        return new TrainerSummaryResponse(workload.getUsername(), workload.getFirstName(),
                workload.getLastName(), workload.isActive(), years);
    }
}
