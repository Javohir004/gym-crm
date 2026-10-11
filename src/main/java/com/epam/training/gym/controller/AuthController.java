package com.epam.training.gym.controller;


import com.epam.training.gym.dto.requests.ChangeLoginRequest;
import com.epam.training.gym.facade.GymFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Tag(name = "Auth", description = "Login and password change, for both Trainees and Trainers")
public class AuthController {

    private final GymFacade facade;

    public AuthController(GymFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/api/login")
    @Operation(summary = "Login", description = "Checks that the username and password match")
    @ApiResponse(responseCode = "200", description = "Credentials match")
    @ApiResponse(responseCode = "401", description = "Invalid username or password")
    public ResponseEntity<Void> login(@RequestParam String username, @RequestParam String password) {
        facade.login(username, password);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/login")
    @Operation(summary = "Change password", description = "Requires the current password")
    @ApiResponse(responseCode = "200", description = "Password changed")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Invalid username or current password")
    public ResponseEntity<Void> changeLogin(@RequestBody ChangeLoginRequest request) {
        facade.changeLogin(request.username(), request.oldPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}