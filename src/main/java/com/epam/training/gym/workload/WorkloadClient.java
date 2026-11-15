package com.epam.training.gym.workload;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "trainer-workload-service", path = "/api/v1/workloads")
public interface WorkloadClient {

    @PostMapping
    void updateWorkload(@RequestBody TrainerWorkloadRequest request);
}
