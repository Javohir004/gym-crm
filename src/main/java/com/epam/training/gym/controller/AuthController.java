package com.epam.training.gym.controller;


import com.epam.training.gym.dto.requests.ChangeLoginRequest;
import com.epam.training.gym.service.AuthenticationService;
import com.epam.training.gym.service.UserAccountService;
import com.epam.training.gym.util.BasicAuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Tag(name = "Auth", description = "Login and password change, for both Trainees and Trainers")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserAccountService userAccountService;

    public AuthController(AuthenticationService authenticationService, UserAccountService userAccountService) {
        this.authenticationService = authenticationService;
        this.userAccountService = userAccountService;
    }

    @GetMapping("/api/login")
    @Operation(summary = "Login",
            description = "Checks that the username and password from the Basic Authorization header match. "
                    + "Credentials are never sent in the URL, so they do not end up in access logs or browser history")
    @ApiResponse(responseCode = "200", description = "Credentials match")
    @ApiResponse(responseCode = "401", description = "Invalid username or password")
    public ResponseEntity<Void> login(HttpServletRequest request) {
        BasicAuthUtil.Credentials credentials = BasicAuthUtil.extract(request);
        authenticationService.authenticate(credentials.username(), credentials.password());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/login")
    @Operation(summary = "Change password", description = "Requires the current password")
    @ApiResponse(responseCode = "200", description = "Password changed")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Invalid username or current password")
    public ResponseEntity<Void> changeLogin(@RequestBody ChangeLoginRequest request) {
        userAccountService.changePassword(request.username(), request.oldPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}
