package com.epam.training.gym.controller;

import com.epam.training.gym.dto.requests.ChangeLoginRequest;

import com.epam.training.gym.dto.requests.LoginRequest;
import com.epam.training.gym.dto.responses.LoginResponse;
import com.epam.training.gym.security.SelfAccess;
import com.epam.training.gym.service.AuthenticationService;
import com.epam.training.gym.service.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;


@RestController
@Tag(name = "Auth", description = "Login and password change, for both Trainees and Trainers. "
        + "Logout is POST /api/logout with the Bearer token.")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserAccountService userAccountService;

    public AuthController(AuthenticationService authenticationService, UserAccountService userAccountService) {
        this.authenticationService = authenticationService;
        this.userAccountService = userAccountService;
    }

    @PostMapping("/api/login")
    @Operation(summary = "Login", description = "Returns a JWT; 3 wrong passwords block the username for 5 minutes",
            security = {})
    @ApiResponse(responseCode = "200", description = "Credentials match, token returned")
    @ApiResponse(responseCode = "401", description = "Invalid username or password")
    @ApiResponse(responseCode = "429", description = "Username is temporarily blocked")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        String token = authenticationService.login(request.username(), request.password());
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PutMapping("/api/login")
    @Operation(summary = "Change password", description = "Requires the current password",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Password changed")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Invalid username or current password")
    @ApiResponse(responseCode = "403", description = "Trying to change someone else's password")
    public ResponseEntity<Void> changeLogin(@RequestBody ChangeLoginRequest request, Principal principal) {
        SelfAccess.require(principal, request.username());
        userAccountService.changePassword(request.username(), request.oldPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}