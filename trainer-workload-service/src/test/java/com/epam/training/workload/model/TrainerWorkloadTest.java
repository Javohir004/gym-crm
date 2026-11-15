package com.epam.training.workload.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerWorkloadTest {

    private final TrainerWorkload workload = new TrainerWorkload("Anna.Lee", "Anna", "Lee", true);

    @Test
    void addMinutes_sumsTrainingsOfTheSameMonth_andKeepsMonthsApart() {
        workload.addMinutes(LocalDate.of(2026, 3, 1), 60);
        workload.addMinutes(LocalDate.of(2026, 3, 20), 45);
        workload.addMinutes(LocalDate.of(2026, 4, 2), 30);

        assertEquals(105, workload.minutesIn(2026, Month.MARCH));
        assertEquals(30, workload.minutesIn(2026, Month.APRIL));
        assertEquals(0, workload.minutesIn(2026, Month.MAY));
        assertEquals(0, workload.minutesIn(2025, Month.MARCH));
    }

    @Test
    void removeMinutes_subtractsFromTheRightMonth() {
        workload.addMinutes(LocalDate.of(2026, 3, 1), 90);

        int removed = workload.removeMinutes(LocalDate.of(2026, 3, 15), 60);

        assertEquals(60, removed);
        assertEquals(30, workload.minutesIn(2026, Month.MARCH));
    }

    @Test
    void removeMinutes_neverGoesBelowZero_andDropsEmptyMonthAndYear() {
        workload.addMinutes(LocalDate.of(2026, 3, 1), 30);

        int removed = workload.removeMinutes(LocalDate.of(2026, 3, 1), 60);

        assertEquals(30, removed);
        assertTrue(workload.getMinutesByYear().isEmpty());
    }

    @Test
    void removeMinutes_returnsZero_whenNothingWasRecordedForThatMonth() {
        workload.addMinutes(LocalDate.of(2026, 3, 1), 30);

        assertEquals(0, workload.removeMinutes(LocalDate.of(2026, 4, 1), 30));
        assertEquals(0, workload.removeMinutes(LocalDate.of(2027, 3, 1), 30));
        assertEquals(30, workload.minutesIn(2026, Month.MARCH));
    }

    @Test
    void updateProfile_replacesNameAndStatus() {
        workload.updateProfile("Annie", "Leigh", false);

        assertEquals("Annie", workload.getFirstName());
        assertEquals("Leigh", workload.getLastName());
        assertFalse(workload.isActive());
        assertEquals("Anna.Lee", workload.getUsername());
    }

    @Test
    void minutesByYear_isReadOnly() {
        workload.addMinutes(LocalDate.of(2026, 3, 1), 30);

        assertThrows(UnsupportedOperationException.class, () -> workload.getMinutesByYear().clear());
    }
}
