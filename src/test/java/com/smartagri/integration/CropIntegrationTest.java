package com.smartagri.integration;

import com.smartagri.domain.dto.*;
import com.smartagri.domain.enums.CropStatus;
import com.smartagri.domain.enums.Role;
import com.smartagri.domain.enums.Season;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CropIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String token;

    @BeforeEach
    public void setUp() {
        // Register and login to get a token
        UserDto userDto = new UserDto();
        userDto.setFullName("Farmer Joe");
        userDto.setEmail("joe@farmer.com");
        userDto.setPassword("farmer123");
        userDto.setRole(Role.FARMER);

        restTemplate.postForEntity("/api/auth/register", userDto, UserDto.class);

        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("joe@farmer.com");
        authRequest.setPassword("farmer123");

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login", authRequest, AuthResponse.class);
        
        token = loginResponse.getBody().getToken();
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    public void testCropLifecycle() {
        // 1. Create a crop
        CropDto cropDto = new CropDto();
        cropDto.setCropName("Wheat");
        cropDto.setCropType("Grain");
        cropDto.setSeason(Season.KHARIF);
        cropDto.setPlantingDate(LocalDate.now());
        cropDto.setAreaInAcres(10.5);

        HttpEntity<CropDto> request = new HttpEntity<>(cropDto, getHeaders());
        ResponseEntity<CropDto> createResponse = restTemplate.postForEntity("/api/crops", request, CropDto.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        CropDto createdCrop = createResponse.getBody();
        assertNotNull(createdCrop.getId());
        assertEquals("Wheat", createdCrop.getCropName());
        assertEquals(CropStatus.PLANTED, createdCrop.getStatus());

        Long cropId = createdCrop.getId();

        // 2. Retrieve the crop
        ResponseEntity<CropDto> getResponse = restTemplate.exchange(
                "/api/crops/" + cropId, HttpMethod.GET, new HttpEntity<>(getHeaders()), CropDto.class);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("Wheat", getResponse.getBody().getCropName());

        // 3. Update status to GROWING
        ResponseEntity<CropDto> statusResponse = restTemplate.exchange(
                "/api/crops/" + cropId + "/status?status=GROWING",
                HttpMethod.PATCH,
                new HttpEntity<>(getHeaders()),
                CropDto.class);
        assertEquals(HttpStatus.OK, statusResponse.getStatusCode());
        assertEquals(CropStatus.GROWING, statusResponse.getBody().getStatus());

        // 4. Delete the crop
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/crops/" + cropId, HttpMethod.DELETE, new HttpEntity<>(getHeaders()), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

        // Verify deletion
        ResponseEntity<CropDto> postDeleteResponse = restTemplate.exchange(
                "/api/crops/" + cropId, HttpMethod.GET, new HttpEntity<>(getHeaders()), CropDto.class);
        assertEquals(HttpStatus.NOT_FOUND, postDeleteResponse.getStatusCode());
    }
}
