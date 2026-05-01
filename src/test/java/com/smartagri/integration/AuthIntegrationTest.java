package com.smartagri.integration;

import com.smartagri.domain.dto.AuthRequest;
import com.smartagri.domain.dto.AuthResponse;
import com.smartagri.domain.dto.UserDto;
import com.smartagri.domain.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuthIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testRegisterAndLogin() {
        // 1. Register a new user
        UserDto userDto = new UserDto();
        userDto.setFullName("John Doe");
        userDto.setEmail("john.doe@example.com");
        userDto.setPassword("password123");
        userDto.setRole(Role.FARMER);

        ResponseEntity<UserDto> registerResponse = restTemplate.postForEntity(
                "/api/auth/register", userDto, UserDto.class);
        
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        assertNotNull(registerResponse.getBody());
        assertEquals("John Doe", registerResponse.getBody().getFullName());
        assertEquals("john.doe@example.com", registerResponse.getBody().getEmail());
        assertNotNull(registerResponse.getBody().getId());

        // 2. Login and receive JWT token
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("john.doe@example.com");
        authRequest.setPassword("password123");

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login", authRequest, AuthResponse.class);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertNotNull(loginResponse.getBody().getToken());
        assertEquals("John Doe", loginResponse.getBody().getUser().getFullName());
    }
}
