package com.smartagri.controller;

import com.smartagri.domain.dto.UserDto;
import com.smartagri.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User profile and admin user management endpoints.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "User profile management")
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users/me — Returns the authenticated user's own profile.
     */
    @GetMapping("/me")
    @Operation(summary = "Get my profile")
    public ResponseEntity<UserDto> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getUserByEmail(userDetails.getUsername()));
    }

    /**
     * PUT /api/users/me — Update the authenticated user's own profile.
     */
    @PutMapping("/me")
    @Operation(summary = "Update my profile")
    public ResponseEntity<UserDto> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserDto userDto) {
        UserDto existing = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(userService.updateUser(existing.getId(), userDto));
    }

    // ─── Admin endpoints ──────────────────────────────────────────────────────

    /**
     * GET /api/users — Returns all users (ADMIN only).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users (Admin)")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * GET /api/users/{id} — Get a user by ID (ADMIN only).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID (Admin)")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * PUT /api/users/{id} — Update any user (ADMIN only).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user (Admin)")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDto userDto) {
        return ResponseEntity.ok(userService.updateUser(id, userDto));
    }

    /**
     * DELETE /api/users/{id} — Disable a user account (ADMIN only).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Disable user (Admin)")
    public ResponseEntity<Void> disableUser(@PathVariable Long id) {
        userService.disableUser(id);
        return ResponseEntity.noContent().build();
    }
}
