package com.epam.training.workload.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Document(collection = TrainerSummary.COLLECTION)
@CompoundIndex(name = TrainerSummary.NAME_INDEX, def = "{'trainerFirstName': 1, 'trainerLastName': 1}")
public class TrainerSummary {

    public static final String COLLECTION = "trainer_summaries";
    public static final String USERNAME_INDEX = "trainer_username_unique";
    public static final String NAME_INDEX = "trainer_first_name_last_name";

    @Id
    private String id;

    @NotBlank
    @Indexed(name = USERNAME_INDEX, unique = true)
    private String trainerUsername;

    @NotBlank
    private String trainerFirstName;

    @NotBlank
    private String trainerLastName;

    @NotNull
    private Boolean trainerStatus;

    @Valid
    @NotNull
    private List<YearSummary> years = new ArrayList<>();

    @Version
    private Long version;

    public TrainerSummary() {
    }

    public TrainerSummary(String trainerUsername, String trainerFirstName, String trainerLastName,
                          boolean trainerStatus) {
        this.trainerUsername = trainerUsername;
        updateProfile(trainerFirstName, trainerLastName, trainerStatus);
    }

    public void updateProfile(String firstName, String lastName, boolean status) {
        this.trainerFirstName = firstName;
        this.trainerLastName = lastName;
        this.trainerStatus = status;
    }

    public long addMinutes(LocalDate trainingDate, int minutes) {
        MonthSummary month = findOrAddYear(trainingDate.getYear()).findOrAddMonth(trainingDate.getMonthValue());
        month.setTrainingSummaryDuration(month.getTrainingSummaryDuration() + minutes);
        return month.getTrainingSummaryDuration();
    }

    public long removeMinutes(LocalDate trainingDate, int minutes) {
        Optional<YearSummary> year = findYear(trainingDate.getYear());
        Optional<MonthSummary> month = year.flatMap(found -> found.findMonth(trainingDate.getMonthValue()));
        if (month.isEmpty()) {
            return 0;
        }
        long current = month.get().getTrainingSummaryDuration();
        long removed = Math.min(current, minutes);
        month.get().setTrainingSummaryDuration(current - removed);
        if (current - removed == 0) {
            year.get().removeMonth(month.get());
            if (year.get().isEmpty()) {
                years.remove(year.get());
            }
        }
        return removed;
    }

    public long minutesIn(int year, int month) {
        return findYear(year)
                .flatMap(found -> found.findMonth(month))
                .map(MonthSummary::getTrainingSummaryDuration)
                .orElse(0L);
    }

    private Optional<YearSummary> findYear(int year) {
        return years.stream().filter(summary -> summary.getYear() == year).findFirst();
    }

    private YearSummary findOrAddYear(int year) {
        return findYear(year).orElseGet(() -> {
            YearSummary created = new YearSummary(year);
            years.add(created);
            years.sort(Comparator.comparing(YearSummary::getYear));
            return created;
        });
    }

    public String getId() {
        return id;
    }

    public String getTrainerUsername() {
        return trainerUsername;
    }

    public String getTrainerFirstName() {
        return trainerFirstName;
    }

    public String getTrainerLastName() {
        return trainerLastName;
    }

    public Boolean getTrainerStatus() {
        return trainerStatus;
    }

    public List<YearSummary> getYears() {
        return List.copyOf(years);
    }

    public Long getVersion() {
        return version;
    }
}
