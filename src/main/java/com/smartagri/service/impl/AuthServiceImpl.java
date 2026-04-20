package com.smartagri.service.impl;

import com.smartagri.domain.dto.AuthRequest;
import com.smartagri.domain.dto.AuthResponse;
import com.smartagri.domain.dto.UserDto;
import com.smartagri.domain.entity.User;
import com.smartagri.domain.enums.Role;
import com.smartagri.exception.ResourceNotFoundException;
import com.smartagri.repository.UserRepository;
import com.smartagri.service.AuthService;
import com.smartagri.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Concrete implementation of {@link AuthService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse login(AuthRequest request) {
        log.debug("Authenticating user: {}", request.getEmail());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getEmail()));

        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .accessToken(token)
                .expiresIn(jwtUtil.getExpirationMs())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public UserDto register(UserDto userDto) {
        log.debug("Registering new user: {}", userDto.getEmail());

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + userDto.getEmail());
        }

        User user = User.builder()
                .fullName(userDto.getFullName())
                .email(userDto.getEmail())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .role(userDto.getRole() != null ? userDto.getRole() : Role.FARMER)
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered with id={}", saved.getId());

        return toDto(saved);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setEnabled(user.isEnabled());
        return dto;
    }
}
