package com.epam.training.gym.controller;

import com.epam.training.gym.dto.DtoMapper;
import com.epam.training.gym.dto.TrainingTypeDto;
import com.epam.training.gym.service.TrainingTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/v1/training-types")
@Tag(name = "Training Type", description = "The constant list of training types")
public class TrainingTypeController {

    private final TrainingTypeService trainingTypeService;

    public TrainingTypeController(TrainingTypeService trainingTypeService) {
        this.trainingTypeService = trainingTypeService;
    }

    @GetMapping
    @Operation(summary = "List all training types")
    @ApiResponse(responseCode = "200", description = "List returned")
    public ResponseEntity<List<TrainingTypeDto>> getAll() {
        return ResponseEntity.ok(DtoMapper.toTrainingTypeDtoList(trainingTypeService.getAll()));
    }
}