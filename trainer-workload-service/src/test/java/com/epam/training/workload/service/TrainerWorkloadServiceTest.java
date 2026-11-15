package com.epam.training.workload.service;

import com.epam.training.workload.dto.TrainerSummaryResponse;
import com.epam.training.workload.dto.WorkloadRequest;
import com.epam.training.workload.exception.NotFoundException;
import com.epam.training.workload.model.ActionType;
import com.epam.training.workload.repository.InMemoryTrainerWorkloadRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerWorkloadServiceTest {

    private final InMemoryTrainerWorkloadRepository repository = new InMemoryTrainerWorkloadRepository();
    private final TrainerWorkloadService service = new TrainerWorkloadService(repository);

    private static WorkloadRequest request(LocalDate date, int minutes, ActionType action) {
        return new WorkloadRequest("Anna.Lee", "Anna", "Lee", true, date, minutes, action);
    }

    @Test
    void add_createsTrainer_andSumsMinutesPerMonth() {
        service.process(request(LocalDate.of(2026, 3, 1), 60, ActionType.ADD));
        service.process(request(LocalDate.of(2026, 3, 9), 30, ActionType.ADD));

        assertEquals(90, service.getMonthlyWorkload("Anna.Lee", 2026, Month.MARCH).trainingSummaryDuration());
    }

    @Test
    void add_keepsTheLatestProfileFromTheEvent() {
        service.process(request(LocalDate.of(2026, 3, 1), 60, ActionType.ADD));
        service.process(new WorkloadRequest("Anna.Lee", "Annie", "Leigh", false,
                LocalDate.of(2026, 3, 2), 15, ActionType.ADD));

        TrainerSummaryResponse summary = service.getSummary("Anna.Lee");
        assertEquals("Annie", summary.trainerFirstName());
        assertEquals("Leigh", summary.trainerLastName());
        assertFalse(summary.trainerStatus());
    }

    @Test
    void delete_subtractsTheCancelledTraining() {
        service.process(request(LocalDate.of(2026, 3, 1), 60, ActionType.ADD));
        service.process(request(LocalDate.of(2026, 3, 9), 30, ActionType.ADD));

        service.process(request(LocalDate.of(2026, 3, 9), 30, ActionType.DELETE));

        assertEquals(60, service.getMonthlyWorkload("Anna.Lee", 2026, Month.MARCH).trainingSummaryDuration());
    }

    @Test
    void delete_ofUnknownTrainer_isIgnored_andCreatesNothing() {
        service.process(request(LocalDate.of(2026, 3, 1), 60, ActionType.DELETE));

        assertTrue(repository.findByUsername("Anna.Lee").isEmpty());
    }

    @Test
    void delete_moreThanRecorded_stopsAtZero() {
        service.process(request(LocalDate.of(2026, 3, 1), 20, ActionType.ADD));

        service.process(request(LocalDate.of(2026, 3, 1), 60, ActionType.DELETE));

        assertEquals(0, service.getMonthlyWorkload("Anna.Lee", 2026, Month.MARCH).trainingSummaryDuration());
    }

    @Test
    void getSummary_listsYearsAndMonthsInOrder() {
        service.process(request(LocalDate.of(2027, 1, 5), 45, ActionType.ADD));
        service.process(request(LocalDate.of(2026, 12, 5), 30, ActionType.ADD));
        service.process(request(LocalDate.of(2026, 2, 5), 60, ActionType.ADD));

        TrainerSummaryResponse summary = service.getSummary("Anna.Lee");

        assertEquals("Anna.Lee", summary.trainerUsername());
        assertTrue(summary.trainerStatus());
        assertEquals(2, summary.years().size());
        assertEquals(2026, summary.years().get(0).year());
        assertEquals(Month.FEBRUARY, summary.years().get(0).months().get(0).month());
        assertEquals(60, summary.years().get(0).months().get(0).trainingSummaryDuration());
        assertEquals(Month.DECEMBER, summary.years().get(0).months().get(1).month());
        assertEquals(2027, summary.years().get(1).year());
        assertEquals(45, summary.years().get(1).months().get(0).trainingSummaryDuration());
    }

    @Test
    void reading_anUnknownTrainer_throwsNotFound() {
        assertThrows(NotFoundException.class, () -> service.getSummary("No.One"));
        assertThrows(NotFoundException.class, () -> service.getMonthlyWorkload("No.One", 2026, Month.MARCH));
    }
}
