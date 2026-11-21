package com.epam.training.workload.controller;

import com.epam.training.workload.dto.MonthlyWorkloadResponse;
import com.epam.training.workload.dto.TrainerSummaryResponse;
import com.epam.training.workload.service.TrainerWorkloadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Month;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workloads")
public class WorkloadController {

    private final TrainerWorkloadService service;

    public WorkloadController(TrainerWorkloadService service) {
        this.service = service;
    }

    @GetMapping(params = {"firstName", "lastName"})
    public ResponseEntity<List<TrainerSummaryResponse>> findByName(@RequestParam String firstName,
                                                                   @RequestParam String lastName) {
        return ResponseEntity.ok(service.findByName(firstName, lastName));
    }

    @GetMapping("/{username}")
    public ResponseEntity<TrainerSummaryResponse> getSummary(@PathVariable String username) {
        return ResponseEntity.ok(service.getSummary(username));
    }

    @GetMapping("/{username}/years/{year}/months/{month}")
    public ResponseEntity<MonthlyWorkloadResponse> getMonthlyWorkload(@PathVariable String username,
                                                                      @PathVariable int year,
                                                                      @PathVariable int month) {
        return ResponseEntity.ok(service.getMonthlyWorkload(username, year, Month.of(month)));
    }
}
