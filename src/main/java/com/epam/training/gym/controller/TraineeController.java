package com.epam.training.gym.controller;

import com.epam.training.gym.dto.DtoMapper;
import com.epam.training.gym.dto.TraineeTrainingDto;
import com.epam.training.gym.dto.TrainerShortDto;
import com.epam.training.gym.dto.requests.ActivateRequest;
import com.epam.training.gym.dto.requests.TraineeRegistrationRequest;
import com.epam.training.gym.dto.requests.UpdateTraineeProfileRequest;
import com.epam.training.gym.dto.requests.UpdateTraineeTrainersRequest;
import com.epam.training.gym.dto.responses.RegistrationResponse;
import com.epam.training.gym.dto.responses.TraineeProfileResponse;
import com.epam.training.gym.dto.responses.UpdateTraineeProfileResponse;
import com.epam.training.gym.dto.responses.UpdateTraineeTrainersResponse;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.facade.GymFacade;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.util.BasicAuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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

/**
 * Endpoints 2, 5, 6, 7, 10, 11, 12, 15 - everything about Trainee profiles.
 * Every endpoint except registration requires HTTP Basic Auth; the
 * {username} in the path must match the authenticated user (you can only
 * act on your own profile).
 */
@RestController
@RequestMapping("/api/trainees")
@Tag(name = "Trainee", description = "Trainee registration, profile, trainers list and trainings")
public class TraineeController {

    private final GymFacade facade;

    public TraineeController(GymFacade facade) {
        this.facade = facade;
    }

    /** 2. Trainee Registration (POST). */
    @PostMapping
    @Operation(summary = "Register a new Trainee")
    @ApiResponse(responseCode = "201", description = "Trainee created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    public ResponseEntity<RegistrationResponse> register(@RequestBody TraineeRegistrationRequest request) {
        Trainee trainee = facade.createTrainee(request.firstName(), request.lastName(),
                request.dateOfBirth(), request.address());
        RegistrationResponse response = new RegistrationResponse(
                trainee.getUser().getUsername(), trainee.getUser().getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** 5. Get Trainee Profile (GET). */
    @GetMapping("/{username}")
    @Operation(summary = "Get a trainee's own profile", security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "basicAuth"))
    @ApiResponse(responseCode = "200", description = "Profile found")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<TraineeProfileResponse> getProfile(
            @PathVariable String username, HttpServletRequest request) {
        String password = authenticate(username, request);
        Trainee trainee = facade.getTrainee(username, password);
        return ResponseEntity.ok(DtoMapper.toTraineeProfileResponse(trainee));
    }

    /** 6. Update Trainee Profile (PUT). */
    @PutMapping("/{username}")
    @Operation(summary = "Update a trainee's own profile")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<UpdateTraineeProfileResponse> updateProfile(
            @PathVariable String username, @RequestBody UpdateTraineeProfileRequest body, HttpServletRequest request) {
        String password = authenticate(username, request, body.username());
        Trainee trainee = facade.updateTraineeProfile(username, password, body.firstName(), body.lastName(),
                body.dateOfBirth(), body.address(), Boolean.TRUE.equals(body.isActive()));
        return ResponseEntity.ok(DtoMapper.toUpdateTraineeProfileResponse(trainee));
    }

    /** 7. Delete Trainee Profile (DELETE): hard delete, cascades to trainings. */
    @DeleteMapping("/{username}")
    @Operation(summary = "Delete a trainee's own profile (hard delete, cascades to its trainings)")
    @ApiResponse(responseCode = "200", description = "Deleted")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<Void> delete(@PathVariable String username, HttpServletRequest request) {
        String password = authenticate(username, request);
        facade.deleteTrainee(username, password);
        return ResponseEntity.ok().build();
    }

    /** 10. Get not assigned active trainers (GET). */
    @GetMapping("/{username}/unassigned-trainers")
    @Operation(summary = "List active trainers not yet assigned to this trainee")
    @ApiResponse(responseCode = "200", description = "List returned")
    public ResponseEntity<java.util.List<TrainerShortDto>> getUnassignedActiveTrainers(
            @PathVariable String username, HttpServletRequest request) {
        String password = authenticate(username, request);
        var trainers = facade.getUnassignedActiveTrainers(username, password);
        return ResponseEntity.ok(DtoMapper.toTrainerShortDtoList(trainers));
    }

    /** 11. Update Trainee's Trainer List (PUT): replaces the whole list. */
    @PutMapping("/{username}/trainers")
    @Operation(summary = "Replace this trainee's trainers with the given list")
    @ApiResponse(responseCode = "200", description = "List updated")
    @ApiResponse(responseCode = "404", description = "One of the given trainer usernames does not exist")
    public ResponseEntity<UpdateTraineeTrainersResponse> updateTrainers(
            @PathVariable String username, @RequestBody UpdateTraineeTrainersRequest body, HttpServletRequest request) {
        String password = authenticate(username, request, body.traineeUsername());
        Trainee trainee = facade.updateTraineeTrainers(username, password, body.trainers());
        return ResponseEntity.ok(new UpdateTraineeTrainersResponse(DtoMapper.toTrainerShortDtoList(trainee.getTrainers())));
    }

    /** 12. Get Trainee Trainings List (GET): every filter is optional. */
    @GetMapping("/{username}/trainings")
    @Operation(summary = "List this trainee's trainings, optionally filtered")
    @ApiResponse(responseCode = "200", description = "List returned")
    public ResponseEntity<java.util.List<TraineeTrainingDto>> getTrainings(
            @PathVariable String username,
            @Parameter(description = "Period start (inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @Parameter(description = "Period end (inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @Parameter(description = "Part of the trainer's name") @RequestParam(required = false) String trainerName,
            @Parameter(description = "Training type name") @RequestParam(required = false) String trainingType,
            HttpServletRequest request) {
        String password = authenticate(username, request);
        var trainings = facade.getTraineeTrainings(username, password, periodFrom, periodTo, trainerName, trainingType);
        return ResponseEntity.ok(DtoMapper.toTraineeTrainingDtoList(trainings));
    }

    /** 15. Activate/De-activate Trainee (PATCH). */
    @PatchMapping("/{username}/status")
    @Operation(summary = "Activate or de-activate this trainee")
    @ApiResponse(responseCode = "200", description = "Status updated")
    public ResponseEntity<Void> setActive(
            @PathVariable String username, @RequestBody ActivateRequest body, HttpServletRequest request) {
        String password = authenticate(username, request, body.username());
        facade.setTraineeActive(username, password, Boolean.TRUE.equals(body.isActive()));
        return ResponseEntity.ok().build();
    }

    // ---------- Basic Auth helpers ----------

    private String authenticate(String pathUsername, HttpServletRequest request) {
        BasicAuthUtil.Credentials credentials = credentials(request);
        requireSameUser(pathUsername, credentials.username());
        return credentials.password();
    }

    private String authenticate(String pathUsername, HttpServletRequest request, String bodyUsername) {
        requireSameUser(pathUsername, bodyUsername);
        return authenticate(pathUsername, request);
    }

    private BasicAuthUtil.Credentials credentials(HttpServletRequest request) {
        return BasicAuthUtil.extract(request);
    }

    private void requireSameUser(String pathUsername, String otherUsername) {
        if (otherUsername != null && !pathUsername.equals(otherUsername)) {
            throw new AuthenticationException("Username in the request does not match the URL");
        }
    }
}