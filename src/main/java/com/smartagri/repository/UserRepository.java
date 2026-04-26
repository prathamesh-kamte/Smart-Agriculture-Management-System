package com.smartagri.repository;

import com.smartagri.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Lookup a user by email (used as the login username).
     */
    Optional<User> findByEmail(String email);

    /**
     * Check whether an email is already registered.
     */
    boolean existsByEmail(String email);
}
