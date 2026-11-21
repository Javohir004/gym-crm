package com.epam.training.workload.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class YearSummary {

    @NotNull
    @Min(1)
    private Integer year;

    @Valid
    @NotNull
    private List<MonthSummary> months = new ArrayList<>();

    public YearSummary() {
    }

    public YearSummary(int year) {
        this.year = year;
    }

    public Integer getYear() {
        return year;
    }

    public List<MonthSummary> getMonths() {
        return List.copyOf(months);
    }

    Optional<MonthSummary> findMonth(int month) {
        return months.stream().filter(summary -> summary.getMonth() == month).findFirst();
    }

    MonthSummary findOrAddMonth(int month) {
        return findMonth(month).orElseGet(() -> {
            MonthSummary created = new MonthSummary(month, 0);
            months.add(created);
            months.sort(Comparator.comparing(MonthSummary::getMonth));
            return created;
        });
    }

    void removeMonth(MonthSummary month) {
        months.remove(month);
    }

    boolean isEmpty() {
        return months.isEmpty();
    }
}
