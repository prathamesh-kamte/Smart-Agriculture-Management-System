package com.smartagri.domain.dto;

import com.smartagri.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for user profile reads and admin-driven writes.
 * Password is excluded from responses (write-only via AuthRequest).
 */
@Data
public class UserDto {

    /** Null on create; populated on responses. */
    private Long id;

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @Email(message = "Must be a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

    /** Only required when creating a new user via admin endpoint. */
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private Role role;

    /** Populated on responses. */
    private LocalDateTime createdAt;
    private boolean enabled;
}
