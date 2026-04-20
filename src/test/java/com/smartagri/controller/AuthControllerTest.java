package com.smartagri.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartagri.domain.dto.AuthRequest;
import com.smartagri.domain.dto.AuthResponse;
import com.smartagri.domain.dto.UserDto;
import com.smartagri.domain.enums.Role;
import com.smartagri.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for {@link AuthController} using MockMvc.
 * The security filter chain is active; CSRF tokens are provided where needed.
 */
@WebMvcTest(AuthController.class)
@ActiveProfiles("dev")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // ─── Login ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/login — valid credentials returns 200 with token")
    void login_validCredentials_returns200() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("farmer@smartagri.com");
        request.setPassword("Farmer@123");

        AuthResponse response = AuthResponse.builder()
                .accessToken("mock.jwt.token")
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .email("farmer@smartagri.com")
                .fullName("Ramesh Kumar")
                .role("FARMER")
                .build();

        when(authService.login(any(AuthRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("farmer@smartagri.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login — missing email returns 400")
    void login_missingEmail_returns400() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setPassword("Farmer@123");
        // email intentionally omitted

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login — invalid email format returns 400")
    void login_invalidEmail_returns400() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("not-an-email");
        request.setPassword("Farmer@123");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ─── Register ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/register — valid payload returns 201")
    void register_validPayload_returns201() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setFullName("New Farmer");
        userDto.setEmail("newfarmer@example.com");
        userDto.setPassword("Secret@123");
        userDto.setRole(Role.FARMER);

        UserDto createdUser = new UserDto();
        createdUser.setId(10L);
        createdUser.setFullName("New Farmer");
        createdUser.setEmail("newfarmer@example.com");
        createdUser.setRole(Role.FARMER);
        createdUser.setEnabled(true);

        when(authService.register(any(UserDto.class))).thenReturn(createdUser);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.email").value("newfarmer@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/register — short password returns 400")
    void register_shortPassword_returns400() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setFullName("New Farmer");
        userDto.setEmail("newfarmer@example.com");
        userDto.setPassword("abc"); // too short

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isBadRequest());
    }
}
