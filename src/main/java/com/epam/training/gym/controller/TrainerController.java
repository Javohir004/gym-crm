package com.epam.training.gym.controller;

import com.epam.training.gym.dto.DtoMapper;
import com.epam.training.gym.dto.TrainerTrainingDto;
import com.epam.training.gym.dto.requests.ActivateRequest;
import com.epam.training.gym.dto.requests.TrainerRegistrationRequest;
import com.epam.training.gym.dto.requests.UpdateTrainerProfileRequest;
import com.epam.training.gym.dto.responses.RegistrationResponse;
import com.epam.training.gym.dto.responses.TrainerProfileResponse;
import com.epam.training.gym.dto.responses.UpdateTrainerProfileResponse;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.facade.GymFacade;
import com.epam.training.gym.model.Trainer;
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

/**
 * Endpoints 1, 8, 9, 13, 16 - everything about Trainer profiles.
 */
@RestController
@RequestMapping("/api/trainers")
@Tag(name = "Trainer", description = "Trainer registration, profile and trainings")
public class TrainerController {

    private final GymFacade facade;

    public TrainerController(GymFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    @Operation(summary = "Register a new Trainer")
    @ApiResponse(responseCode = "201", description = "Trainer created")
    @ApiResponse(responseCode = "400", description = "Validation error, or unknown specialization")
    public ResponseEntity<RegistrationResponse> register(@RequestBody TrainerRegistrationRequest request) {
        Trainer trainer = facade.createTrainer(request.firstName(), request.lastName(), request.specialization());
        RegistrationResponse response = new RegistrationResponse(
                trainer.getUser().getUsername(), trainer.getUser().getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get a trainer's own profile")
    @ApiResponse(responseCode = "200", description = "Profile found")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<TrainerProfileResponse> getProfile(
            @PathVariable String username, HttpServletRequest request) {
        String password = authenticate(username, request);
        Trainer trainer = facade.getTrainer(username, password);
        return ResponseEntity.ok(DtoMapper.toTrainerProfileResponse(trainer));
    }

    @PutMapping("/{username}")
    @Operation(summary = "Update a trainer's own profile (specialization cannot be changed here)")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    public ResponseEntity<UpdateTrainerProfileResponse> updateProfile(
            @PathVariable String username, @RequestBody UpdateTrainerProfileRequest body, HttpServletRequest request) {
        String password = authenticate(username, request, body.username());
        Validation.requireNotNull(body.isActive(), "Is Active");
        Trainer trainer = facade.updateTrainerProfile(username, password, body.firstName(), body.lastName(),
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
        String password = authenticate(username, request);
        var trainings = facade.getTrainerTrainings(username, password, periodFrom, periodTo, traineeName);
        return ResponseEntity.ok(DtoMapper.toTrainerTrainingDtoList(trainings));
    }

    /** 16. Activate/De-activate Trainer (PATCH). */
    @PatchMapping("/{username}/status")
    @Operation(summary = "Activate or de-activate this trainer")
    @ApiResponse(responseCode = "200", description = "Status updated")
    public ResponseEntity<Void> setActive(
            @PathVariable String username, @RequestBody ActivateRequest body, HttpServletRequest request) {
        String password = authenticate(username, request, body.username());
        Validation.requireNotNull(body.isActive(), "Is Active");
        facade.setTrainerActive(username, password, body.isActive());
        return ResponseEntity.ok().build();
    }

    // ---------- Basic Auth helpers ----------

    private String authenticate(String pathUsername, HttpServletRequest request) {
        BasicAuthUtil.Credentials credentials = BasicAuthUtil.extract(request);
        requireSameUser(pathUsername, credentials.username());
        return credentials.password();
    }

    private String authenticate(String pathUsername, HttpServletRequest request, String bodyUsername) {
        requireSameUser(pathUsername, bodyUsername);
        return authenticate(pathUsername, request);
    }

    private void requireSameUser(String pathUsername, String otherUsername) {
        if (otherUsername != null && !pathUsername.equals(otherUsername)) {
            throw new AuthenticationException("Username in the request does not match the URL");
        }
    }
}