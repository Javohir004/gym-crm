package com.epam.training.gym.controller;

import com.epam.training.gym.dto.requests.AddTrainingRequest;
import com.epam.training.gym.service.TrainingService;
import com.epam.training.gym.util.BasicAuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/trainings")
@Tag(name = "Training", description = "Adding a training session")
public class TrainingController {

    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @PostMapping
    @Operation(summary = "Add a training",
            description = "Training Type is not part of the request - it is taken from the trainer's specialization")
    @ApiResponse(responseCode = "200", description = "Training added")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "Trainee or trainer not found")
    public ResponseEntity<Void> addTraining(@RequestBody AddTrainingRequest body, HttpServletRequest request) {
        BasicAuthUtil.Credentials credentials = BasicAuthUtil.extract(request);
        trainingService.addTraining(credentials.username(), credentials.password(),
                body.traineeUsername(), body.trainerUsername(), body.trainingName(),
                body.trainingDate(), body.trainingDuration());
        return ResponseEntity.ok().build();
    }
}