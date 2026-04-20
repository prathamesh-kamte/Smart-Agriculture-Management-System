package com.smartagri.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload returned after a successful authentication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** Bearer token to include in subsequent requests. */
    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    /** Token lifetime in milliseconds. */
    private long expiresIn;

    private String email;
    private String fullName;
    private String role;
}
