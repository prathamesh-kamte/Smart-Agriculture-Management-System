package com.smartagri.service;

import com.smartagri.domain.dto.AuthRequest;
import com.smartagri.domain.dto.AuthResponse;
import com.smartagri.domain.dto.UserDto;

/**
 * Contract for authentication operations.
 */
public interface AuthService {

    /**
     * Authenticates a user by email/password and returns a signed JWT.
     *
     * @param request login credentials
     * @return {@link AuthResponse} containing the access token and user metadata
     */
    AuthResponse login(AuthRequest request);

    /**
     * Registers a new farmer account.
     *
     * @param userDto user registration data
     * @return the persisted user as a DTO (password excluded)
     */
    UserDto register(UserDto userDto);
}
