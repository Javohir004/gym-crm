package com.epam.training.workload.service;

import com.epam.training.workload.dto.TrainerSummaryResponse;
import com.epam.training.workload.dto.WorkloadRequest;
import com.epam.training.workload.exception.NotFoundException;
import com.epam.training.workload.model.ActionType;
import com.epam.training.workload.model.TrainerSummary;
import com.epam.training.workload.repository.TrainerSummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadServiceTest {

    private static final LocalDate MARCH_9 = LocalDate.of(2026, 3, 9);

    @Mock
    private TrainerSummaryRepository repository;

    @InjectMocks
    private TrainerWorkloadService service;

    private static WorkloadRequest request(int minutes, ActionType action) {
        return new WorkloadRequest("Anna.Lee", "Anna", "Lee", true, MARCH_9, minutes, action);
    }

    private TrainerSummary saved() {
        ArgumentCaptor<TrainerSummary> captor = ArgumentCaptor.forClass(TrainerSummary.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void add_forUnknownTrainer_createsDocumentWithYearMonthAndDuration() {
        when(repository.findByTrainerUsername("Anna.Lee")).thenReturn(Optional.empty());

        service.process(request(60, ActionType.ADD));

        TrainerSummary created = saved();
        assertEquals("Anna.Lee", created.getTrainerUsername());
        assertEquals("Anna", created.getTrainerFirstName());
        assertEquals(2026, created.getYears().get(0).getYear());
        assertEquals(3, created.getYears().get(0).getMonths().get(0).getMonth());
        assertEquals(60, created.minutesIn(2026, 3));
    }

    @Test
    void add_forExistingTrainer_addsTheDurationToTheFoundMonth_andSavesTheSameDocument() {
        TrainerSummary existing = new TrainerSummary("Anna.Lee", "Anna", "Lee", true);
        existing.addMinutes(MARCH_9, 30);
        when(repository.findByTrainerUsername("Anna.Lee")).thenReturn(Optional.of(existing));

        service.process(request(45, ActionType.ADD));

        assertSame(existing, saved());
        assertEquals(75, existing.minutesIn(2026, 3));
    }

    @Test
    void add_updatesTheProfileFromTheEvent() {
        TrainerSummary existing = new TrainerSummary("Anna.Lee", "Anna", "Lee", true);
        when(repository.findByTrainerUsername("Anna.Lee")).thenReturn(Optional.of(existing));

        service.process(new WorkloadRequest("Anna.Lee", "Annie", "Leigh", false, MARCH_9, 15, ActionType.ADD));

        assertEquals("Annie", existing.getTrainerFirstName());
        assertFalse(existing.getTrainerStatus());
    }

    @Test
    void delete_subtractsTheCancelledTraining_andSaves() {
        TrainerSummary existing = new TrainerSummary("Anna.Lee", "Anna", "Lee", true);
        existing.addMinutes(MARCH_9, 90);
        when(repository.findByTrainerUsername("Anna.Lee")).thenReturn(Optional.of(existing));

        service.process(request(30, ActionType.DELETE));

        assertSame(existing, saved());
        assertEquals(60, existing.minutesIn(2026, 3));
    }

    @Test
    void delete_moreThanRecorded_stopsAtZero() {
        TrainerSummary existing = new TrainerSummary("Anna.Lee", "Anna", "Lee", true);
        existing.addMinutes(MARCH_9, 20);
        when(repository.findByTrainerUsername("Anna.Lee")).thenReturn(Optional.of(existing));

        service.process(request(60, ActionType.DELETE));

        assertEquals(0, existing.minutesIn(2026, 3));
    }

    @Test
    void delete_ofUnknownTrainer_savesNothing() {
        when(repository.findByTrainerUsername("Anna.Lee")).thenReturn(Optional.empty());

        service.process(request(60, ActionType.DELETE));

        verify(repository, never()).save(any());
    }

    @Test
    void getSummary_andMonthlyWorkload_readTheDocument() {
        TrainerSummary existing = new TrainerSummary("Anna.Lee", "Anna", "Lee", true);
        existing.addMinutes(MARCH_9, 60);
        when(repository.findByTrainerUsername("Anna.Lee")).thenReturn(Optional.of(existing));

        TrainerSummaryResponse summary = service.getSummary("Anna.Lee");

        assertEquals(Month.MARCH, summary.years().get(0).months().get(0).month());
        assertEquals(60, summary.years().get(0).months().get(0).trainingSummaryDuration());
        assertEquals(60, service.getMonthlyWorkload("Anna.Lee", 2026, Month.MARCH).trainingSummaryDuration());
        assertEquals(0, service.getMonthlyWorkload("Anna.Lee", 2026, Month.APRIL).trainingSummaryDuration());
    }

    @Test
    void findByName_mapsEveryMatchingDocument() {
        when(repository.findByTrainerFirstNameAndTrainerLastName("Anna", "Lee"))
                .thenReturn(List.of(new TrainerSummary("Anna.Lee", "Anna", "Lee", true),
                        new TrainerSummary("Anna.Lee1", "Anna", "Lee", false)));

        List<TrainerSummaryResponse> found = service.findByName("Anna", "Lee");

        assertEquals(List.of("Anna.Lee", "Anna.Lee1"), found.stream().map(TrainerSummaryResponse::trainerUsername).toList());
    }

    @Test
    void reading_anUnknownTrainer_throwsNotFound() {
        when(repository.findByTrainerUsername("No.One")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getSummary("No.One"));
        assertThrows(NotFoundException.class, () -> service.getMonthlyWorkload("No.One", 2026, Month.MARCH));
    }
}
