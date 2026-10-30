package com.epam.training.gym.controller;

import com.epam.training.gym.dto.DtoMapper;
import com.epam.training.gym.dto.TrainingTypeDto;
import com.epam.training.gym.facade.GymFacade;
import com.epam.training.gym.util.BasicAuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/training-types")
@Tag(name = "Training Type", description = "The constant list of training types")
public class TrainingTypeController {

    private final GymFacade facade;

    public TrainingTypeController(GymFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    @Operation(summary = "List all training types")
    @ApiResponse(responseCode = "200", description = "List returned")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<List<TrainingTypeDto>> getAll(HttpServletRequest request) {
        BasicAuthUtil.Credentials credentials = BasicAuthUtil.extract(request);
        return ResponseEntity.ok(DtoMapper.toTrainingTypeDtoList(
                facade.getTrainingTypes(credentials.username(), credentials.password())));
    }
}
