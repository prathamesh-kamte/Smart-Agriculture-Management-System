package com.smartagri.controller;

import com.smartagri.domain.dto.AuthRequest;
import com.smartagri.domain.dto.AuthResponse;
import com.smartagri.domain.dto.UserDto;
import com.smartagri.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Handles public authentication endpoints: login and registration.
 * No JWT is required to reach these routes.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and user registration endpoints")
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/login
     * Authenticate with email + password and receive a JWT.
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and receive a JWT access token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * POST /api/auth/register
     * Register a new FARMER account. Returns 201 Created with the user profile.
     */
    @PostMapping("/register")
    @Operation(summary = "Register", description = "Register a new farmer account")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserDto> register(@Valid @RequestBody UserDto userDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(userDto));
    }
}
