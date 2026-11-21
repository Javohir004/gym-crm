package com.epam.training.workload.repository;

import com.epam.training.workload.model.TrainerSummary;
import jakarta.validation.ConstraintViolationException;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class TrainerSummaryRepositoryIntegrationTest {

    @Autowired
    private TrainerSummaryRepository repository;
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanCollection() {
        repository.deleteAll();
    }

    @Test
    void savedDocument_isFoundByUsername_withItsYearsAndMonths() {
        TrainerSummary summary = new TrainerSummary("Repo.Trainer", "Repo", "Trainer", true);
        summary.addMinutes(LocalDate.of(2026, 3, 1), 60);
        repository.save(summary);

        TrainerSummary found = repository.findByTrainerUsername("Repo.Trainer").orElseThrow();

        assertEquals(60, found.minutesIn(2026, 3));
        assertEquals(Boolean.TRUE, found.getTrainerStatus());
    }

    @Test
    void updatedDocument_isSavedOverTheSameRecord() {
        repository.save(new TrainerSummary("Update.Trainer", "Update", "Trainer", true));
        TrainerSummary found = repository.findByTrainerUsername("Update.Trainer").orElseThrow();

        found.addMinutes(LocalDate.of(2026, 3, 1), 45);
        repository.save(found);

        assertEquals(1, repository.count());
        assertEquals(45, repository.findByTrainerUsername("Update.Trainer").orElseThrow().minutesIn(2026, 3));
    }

    @Test
    void unknownUsername_isNotFound() {
        assertTrue(repository.findByTrainerUsername("No.Such.Trainer").isEmpty());
    }

    @Test
    void firstNameLastNameIndex_existsInThatFieldOrder() {
        List<IndexInfo> indexes = mongoTemplate.indexOps(TrainerSummary.class).getIndexInfo();

        IndexInfo nameIndex = indexes.stream()
                .filter(index -> index.getName().equals(TrainerSummary.NAME_INDEX))
                .findFirst().orElseThrow();
        assertEquals(List.of("trainerFirstName", "trainerLastName"),
                nameIndex.getIndexFields().stream().map(IndexField::getKey).toList());
    }

    @Test
    void searchByFirstAndLastName_isServedByTheIndex() {
        repository.save(new TrainerSummary("Anna.Lee", "Anna", "Lee", true));
        repository.save(new TrainerSummary("Anna.Smith", "Anna", "Smith", true));

        Document plan = mongoTemplate.getCollection(TrainerSummary.COLLECTION)
                .find(new Document("trainerFirstName", "Anna").append("trainerLastName", "Lee"))
                .explain();

        assertTrue(plan.toJson().contains(TrainerSummary.NAME_INDEX));
    }

    @Test
    void searchByFirstAndLastName_returnsOnlyMatchingTrainers() {
        repository.save(new TrainerSummary("Anna.Lee", "Anna", "Lee", true));
        repository.save(new TrainerSummary("Anna.Lee1", "Anna", "Lee", false));
        repository.save(new TrainerSummary("Anna.Smith", "Anna", "Smith", true));

        List<TrainerSummary> found = repository.findByTrainerFirstNameAndTrainerLastName("Anna", "Lee");

        assertEquals(List.of("Anna.Lee", "Anna.Lee1"),
                found.stream().map(TrainerSummary::getTrainerUsername).sorted().toList());
    }

    @Test
    void usernameIndex_isUnique_andRejectsASecondDocument() {
        IndexInfo usernameIndex = mongoTemplate.indexOps(TrainerSummary.class).getIndexInfo().stream()
                .filter(index -> index.getName().equals(TrainerSummary.USERNAME_INDEX))
                .findFirst().orElseThrow();
        assertTrue(usernameIndex.isUnique());

        repository.save(new TrainerSummary("Twice.Trainer", "Twice", "Trainer", true));
        assertThrows(DuplicateKeyException.class,
                () -> repository.save(new TrainerSummary("Twice.Trainer", "Twice", "Trainer", true)));
    }

    @Test
    void wrongFieldTypes_areRejectedByTheCollectionsJsonSchema() {
        Document wrongTypes = new Document("trainerUsername", "Raw.Trainer")
                .append("trainerFirstName", "Raw")
                .append("trainerLastName", "Trainer")
                .append("trainerStatus", "yes")
                .append("years", List.of(new Document("year", 2026)
                        .append("months", List.of(new Document("month", 3).append("trainingSummaryDuration", "60")))));

        assertThrows(DataIntegrityViolationException.class,
                () -> mongoTemplate.insert(wrongTypes, TrainerSummary.COLLECTION));
    }

    @Test
    void concurrentUpdateOfAStaleCopy_failsWithOptimisticLocking() {
        repository.save(new TrainerSummary("Version.Trainer", "Version", "Trainer", true));
        TrainerSummary first = repository.findByTrainerUsername("Version.Trainer").orElseThrow();
        TrainerSummary stale = repository.findByTrainerUsername("Version.Trainer").orElseThrow();
        first.addMinutes(LocalDate.of(2026, 3, 1), 30);
        repository.save(first);

        stale.addMinutes(LocalDate.of(2026, 3, 1), 45);
        assertThrows(OptimisticLockingFailureException.class, () -> repository.save(stale));
    }

    @Test
    void documentMissingRequiredFields_isRejected_beforeReachingMongo() {
        assertThrows(ConstraintViolationException.class,
                () -> repository.save(new TrainerSummary(" ", "No", "Name", true)));
        assertEquals(0, repository.count());
    }
}
