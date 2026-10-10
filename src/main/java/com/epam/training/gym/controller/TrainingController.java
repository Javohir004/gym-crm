package com.epam.training.gym.controller;

import com.epam.training.gym.dto.requests.AddTrainingRequest;
import com.epam.training.gym.facade.GymFacade;
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

/**
 * Endpoint 14: Add Training. The request carries no Training Type - it is
 * taken from the trainer's own specialization (see TrainingService).
 * Either the trainee or the trainer involved may call this (any
 * authenticated user, per task note 3).
 */
@RestController
@RequestMapping("/api/trainings")
@Tag(name = "Training", description = "Adding a training session")
public class TrainingController {

    private final GymFacade facade;

    public TrainingController(GymFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    @Operation(summary = "Add a training",
            description = "Training Type is not part of the request - it is taken from the trainer's specialization")
    @ApiResponse(responseCode = "200", description = "Training added")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "Trainee or trainer not found")
    public ResponseEntity<Void> addTraining(@RequestBody AddTrainingRequest body, HttpServletRequest request) {
        BasicAuthUtil.Credentials credentials = BasicAuthUtil.extract(request);
        facade.addTraining(credentials.username(), credentials.password(),
                body.traineeUsername(), body.trainerUsername(), body.trainingName(),
                body.trainingDate(), body.trainingDuration());
        return ResponseEntity.ok().build();
    }
}