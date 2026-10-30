package com.epam.training.gym.controller;

import com.epam.training.gym.dto.DtoMapper;
import com.epam.training.gym.dto.TrainerTrainingDto;
import com.epam.training.gym.dto.requests.ActivateRequest;
import com.epam.training.gym.dto.requests.TrainerRegistrationRequest;
import com.epam.training.gym.dto.requests.UpdateTrainerProfileRequest;
import com.epam.training.gym.dto.responses.RegistrationResponse;
import com.epam.training.gym.dto.responses.TrainerProfileResponse;
import com.epam.training.gym.dto.responses.UpdateTrainerProfileResponse;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.service.GeneratedCredentials;
import com.epam.training.gym.service.TrainerService;
import com.epam.training.gym.service.TrainingService;
import com.epam.training.gym.util.BasicAuthUtil;
import com.epam.training.gym.util.Validation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trainers")
@Tag(name = "Trainer", description = "Trainer registration, profile and trainings")
public class TrainerController {

    private final TrainerService trainerService;
    private final TrainingService trainingService;

    public TrainerController(TrainerService trainerService, TrainingService trainingService) {
        this.trainerService = trainerService;
        this.trainingService = trainingService;
    }

    @PostMapping
    @Operation(summary = "Register a new Trainer")
    @ApiResponse(responseCode = "201", description = "Trainer created")
    @ApiResponse(responseCode = "400", description = "Validation error, or unknown specialization")
    public ResponseEntity<RegistrationResponse> register(@RequestBody TrainerRegistrationRequest request) {
        GeneratedCredentials credentials = trainerService.createTrainer(
                request.firstName(), request.lastName(), request.specialization());
        RegistrationResponse response = new RegistrationResponse(credentials.username(), credentials.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get a trainer's own profile")
    @ApiResponse(responseCode = "200", description = "Profile found")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<TrainerProfileResponse> getProfile(
            @PathVariable String username, HttpServletRequest request) {
        String password = BasicAuthUtil.extractFor(request, username).password();
        Trainer trainer = trainerService.getByUsername(username, password);
        return ResponseEntity.ok(DtoMapper.toTrainerProfileResponse(trainer));
    }

    @PutMapping("/{username}")
    @Operation(summary = "Update a trainer's own profile (specialization cannot be changed here)")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    public ResponseEntity<UpdateTrainerProfileResponse> updateProfile(
            @PathVariable String username, @RequestBody UpdateTrainerProfileRequest body, HttpServletRequest request) {
        String password = BasicAuthUtil.extractFor(request, username, body.username()).password();
        Validation.requireNotNull(body.isActive(), "Is Active");
        Trainer trainer = trainerService.updateFullProfile(username, password, body.firstName(), body.lastName(),
                body.isActive());
        return ResponseEntity.ok(DtoMapper.toUpdateTrainerProfileResponse(trainer));
    }

    @GetMapping("/{username}/trainings")
    @Operation(summary = "List this trainer's trainings, optionally filtered")
    @ApiResponse(responseCode = "200", description = "List returned")
    public ResponseEntity<List<TrainerTrainingDto>> getTrainings(
            @PathVariable String username,
            @Parameter(description = "Period start (inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @Parameter(description = "Period end (inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @Parameter(description = "Part of the trainee's name") @RequestParam(required = false) String traineeName,
            HttpServletRequest request) {
        String password = BasicAuthUtil.extractFor(request, username).password();
        var trainings = trainingService.getTrainerTrainings(username, password, periodFrom, periodTo, traineeName);
        return ResponseEntity.ok(DtoMapper.toTrainerTrainingDtoList(trainings));
    }

    @PatchMapping("/{username}/status")
    @Operation(summary = "Activate or de-activate this trainer")
    @ApiResponse(responseCode = "200", description = "Status updated")
    public ResponseEntity<Void> setActive(
            @PathVariable String username, @RequestBody ActivateRequest body, HttpServletRequest request) {
        String password = BasicAuthUtil.extractFor(request, username, body.username()).password();
        Validation.requireNotNull(body.isActive(), "Is Active");
        trainerService.setActive(username, password, body.isActive());
        return ResponseEntity.ok().build();
    }
}
