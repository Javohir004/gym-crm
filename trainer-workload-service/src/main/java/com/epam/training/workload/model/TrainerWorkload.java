package com.epam.training.workload.model;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class TrainerWorkload {

    private final String username;
    private String firstName;
    private String lastName;
    private boolean active;
    private final NavigableMap<Integer, Map<Month, Integer>> minutesByYear = new TreeMap<>();

    public TrainerWorkload(String username, String firstName, String lastName, boolean active) {
        this.username = username;
        updateProfile(firstName, lastName, active);
    }

    public void updateProfile(String firstName, String lastName, boolean active) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.active = active;
    }

    public void addMinutes(LocalDate date, int minutes) {
        minutesByYear.computeIfAbsent(date.getYear(), year -> new EnumMap<>(Month.class))
                .merge(date.getMonth(), minutes, Integer::sum);
    }

    public int removeMinutes(LocalDate date, int minutes) {
        Map<Month, Integer> months = minutesByYear.get(date.getYear());
        if (months == null || !months.containsKey(date.getMonth())) {
            return 0;
        }
        int current = months.get(date.getMonth());
        int removed = Math.min(current, minutes);
        if (current - removed == 0) {
            months.remove(date.getMonth());
            if (months.isEmpty()) {
                minutesByYear.remove(date.getYear());
            }
        } else {
            months.put(date.getMonth(), current - removed);
        }
        return removed;
    }

    public int minutesIn(int year, Month month) {
        return minutesByYear.getOrDefault(year, Map.of()).getOrDefault(month, 0);
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public boolean isActive() {
        return active;
    }

    public NavigableMap<Integer, Map<Month, Integer>> getMinutesByYear() {
        return Collections.unmodifiableNavigableMap(minutesByYear);
    }
}
