package com.epam.training.workload.repository;

import com.epam.training.workload.model.TrainerWorkload;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryTrainerWorkloadRepositoryTest {

    private final InMemoryTrainerWorkloadRepository repository = new InMemoryTrainerWorkloadRepository();

    @Test
    void findByUsername_isEmpty_forUnknownTrainer() {
        assertTrue(repository.findByUsername("No.One").isEmpty());
    }

    @Test
    void update_storesWhatTheUpdaterReturns() {
        TrainerWorkload created = repository.update("Anna.Lee",
                current -> new TrainerWorkload("Anna.Lee", "Anna", "Lee", true));

        assertSame(created, repository.findByUsername("Anna.Lee").orElseThrow());
    }

    @Test
    void update_returningNull_storesNothing() {
        repository.update("No.One", current -> null);

        assertTrue(repository.findByUsername("No.One").isEmpty());
    }

    @Test
    void update_isAtomic_underConcurrentEventsForOneTrainer() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        IntStream.range(0, 400).forEach(i -> pool.submit(() -> repository.update("Anna.Lee", current -> {
            TrainerWorkload workload = current != null ? current : new TrainerWorkload("Anna.Lee", "Anna", "Lee", true);
            workload.addMinutes(LocalDate.of(2026, 3, 1), 1);
            return workload;
        })));
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(400, repository.findByUsername("Anna.Lee").orElseThrow().minutesIn(2026, Month.MARCH));
    }
}
