package com.epam.training.workload.dto;

import com.epam.training.workload.model.TrainerSummary;

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

    public record MonthSummary(Month month, long trainingSummaryDuration) {
    }

    public static TrainerSummaryResponse from(TrainerSummary summary) {
        List<YearSummary> years = summary.getYears().stream()
                .map(year -> new YearSummary(year.getYear(), year.getMonths().stream()
                        .map(month -> new MonthSummary(Month.of(month.getMonth()), month.getTrainingSummaryDuration()))
                        .toList()))
                .toList();
        return new TrainerSummaryResponse(summary.getTrainerUsername(), summary.getTrainerFirstName(),
                summary.getTrainerLastName(), summary.getTrainerStatus(), years);
    }
}
