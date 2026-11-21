package com.epam.training.workload.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class MonthSummary {

    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;

    @NotNull
    @PositiveOrZero
    private Long trainingSummaryDuration;

    public MonthSummary() {
    }

    public MonthSummary(int month, long trainingSummaryDuration) {
        this.month = month;
        this.trainingSummaryDuration = trainingSummaryDuration;
    }

    public Integer getMonth() {
        return month;
    }

    public Long getTrainingSummaryDuration() {
        return trainingSummaryDuration;
    }

    void setTrainingSummaryDuration(long trainingSummaryDuration) {
        this.trainingSummaryDuration = trainingSummaryDuration;
    }
}
