package com.epam.training.workload.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerSummaryTest {

    private final TrainerSummary summary = new TrainerSummary("Anna.Lee", "Anna", "Lee", true);

    @Test
    void addMinutes_createsYearAndMonth_thenSumsDurationsOfTheSameMonth() {
        assertEquals(60, summary.addMinutes(LocalDate.of(2026, 3, 1), 60));
        assertEquals(105, summary.addMinutes(LocalDate.of(2026, 3, 20), 45));

        assertEquals(105, summary.minutesIn(2026, 3));
        assertEquals(1, summary.getYears().size());
        assertEquals(1, summary.getYears().get(0).getMonths().size());
    }

    @Test
    void yearsAndMonths_areKeptInCalendarOrder() {
        summary.addMinutes(LocalDate.of(2027, 1, 5), 10);
        summary.addMinutes(LocalDate.of(2026, 12, 5), 20);
        summary.addMinutes(LocalDate.of(2026, 2, 5), 30);

        List<YearSummary> years = summary.getYears();
        assertEquals(List.of(2026, 2027), years.stream().map(YearSummary::getYear).toList());
        assertEquals(List.of(2, 12), years.get(0).getMonths().stream().map(MonthSummary::getMonth).toList());
    }

    @Test
    void removeMinutes_subtractsFromTheRightMonth() {
        summary.addMinutes(LocalDate.of(2026, 3, 1), 90);

        assertEquals(60, summary.removeMinutes(LocalDate.of(2026, 3, 15), 60));
        assertEquals(30, summary.minutesIn(2026, 3));
    }

    @Test
    void removeMinutes_neverGoesBelowZero_andDropsTheEmptyMonthAndYear() {
        summary.addMinutes(LocalDate.of(2026, 3, 1), 30);

        assertEquals(30, summary.removeMinutes(LocalDate.of(2026, 3, 1), 60));
        assertTrue(summary.getYears().isEmpty());
    }

    @Test
    void removeMinutes_keepsTheYear_whileOtherMonthsRemain() {
        summary.addMinutes(LocalDate.of(2026, 3, 1), 30);
        summary.addMinutes(LocalDate.of(2026, 4, 1), 40);

        summary.removeMinutes(LocalDate.of(2026, 3, 1), 30);

        assertEquals(1, summary.getYears().size());
        assertEquals(40, summary.minutesIn(2026, 4));
    }

    @Test
    void removeMinutes_returnsZero_whenNothingWasRecordedForThatMonth() {
        summary.addMinutes(LocalDate.of(2026, 3, 1), 30);

        assertEquals(0, summary.removeMinutes(LocalDate.of(2026, 4, 1), 30));
        assertEquals(0, summary.removeMinutes(LocalDate.of(2027, 3, 1), 30));
        assertEquals(30, summary.minutesIn(2026, 3));
    }

    @Test
    void updateProfile_replacesNameAndStatus_butNotTheUsername() {
        summary.updateProfile("Annie", "Leigh", false);

        assertEquals("Annie", summary.getTrainerFirstName());
        assertEquals("Leigh", summary.getTrainerLastName());
        assertFalse(summary.getTrainerStatus());
        assertEquals("Anna.Lee", summary.getTrainerUsername());
    }

    @Test
    void exposedLists_areReadOnly() {
        summary.addMinutes(LocalDate.of(2026, 3, 1), 30);

        assertThrows(UnsupportedOperationException.class, () -> summary.getYears().clear());
        assertThrows(UnsupportedOperationException.class, () -> summary.getYears().get(0).getMonths().clear());
    }
}
