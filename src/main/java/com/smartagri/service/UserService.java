package com.smartagri.service;

import com.smartagri.domain.dto.UserDto;

import java.util.List;

/**
 * Contract for user management operations.
 */
public interface UserService {

    /** Retrieve a user by their primary key. */
    UserDto getUserById(Long id);

    /** Retrieve a user by their email address. */
    UserDto getUserByEmail(String email);

    /** Return all registered users (admin only). */
    List<UserDto> getAllUsers();

    /** Update mutable profile fields for a user. */
    UserDto updateUser(Long id, UserDto userDto);

    /** Soft-disable (deactivate) a user account. */
    void disableUser(Long id);

    /** Permanently delete a user account (admin only). */
    void deleteUser(Long id);
}
