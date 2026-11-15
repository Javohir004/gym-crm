package com.epam.training.workload.controller;

import com.epam.training.workload.dto.MonthlyWorkloadResponse;
import com.epam.training.workload.dto.TrainerSummaryResponse;
import com.epam.training.workload.dto.WorkloadRequest;
import com.epam.training.workload.service.TrainerWorkloadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Month;

@RestController
@RequestMapping("/api/v1/workloads")
public class WorkloadController {

    private final TrainerWorkloadService service;

    public WorkloadController(TrainerWorkloadService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> updateWorkload(@Valid @RequestBody WorkloadRequest request) {
        service.process(request);
        return ResponseEntity.ok().build();
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
