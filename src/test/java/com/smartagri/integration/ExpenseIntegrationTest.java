package com.smartagri.integration;

import com.smartagri.domain.dto.*;
import com.smartagri.domain.enums.Role;
import com.smartagri.domain.enums.Season;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ExpenseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String token;
    private Long cropId;

    @BeforeEach
    public void setUp() {
        // 1. Register and login
        UserDto userDto = new UserDto();
        userDto.setFullName("Expense Tester");
        userDto.setEmail("tester@expense.com");
        userDto.setPassword("test1234");
        userDto.setRole(Role.FARMER);
        restTemplate.postForEntity("/api/auth/register", userDto, UserDto.class);

        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("tester@expense.com");
        authRequest.setPassword("test1234");
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login", authRequest, AuthResponse.class);
        token = loginResponse.getBody().getToken();

        // 2. Create a crop to associate expenses with
        CropDto cropDto = new CropDto();
        cropDto.setCropName("Corn");
        cropDto.setCropType("Grain");
        cropDto.setSeason(Season.KHARIF);
        cropDto.setPlantingDate(LocalDate.now());
        cropDto.setAreaInAcres(5.0);

        ResponseEntity<CropDto> cropResponse = restTemplate.postForEntity(
                "/api/crops", new HttpEntity<>(cropDto, getHeaders()), CropDto.class);
        cropId = cropResponse.getBody().getId();
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    public void testExpenseLifecycle() {
        // 1. Log an expense
        ExpenseDto expenseDto = new ExpenseDto();
        expenseDto.setDescription("Seeds");
        expenseDto.setCategory("Input");
        expenseDto.setAmount(new BigDecimal("500.00"));
        expenseDto.setExpenseDate(LocalDate.now());
        expenseDto.setCropId(cropId);

        ResponseEntity<ExpenseDto> createResponse = restTemplate.postForEntity(
                "/api/expenses", new HttpEntity<>(expenseDto, getHeaders()), ExpenseDto.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        ExpenseDto createdExpense = createResponse.getBody();
        assertNotNull(createdExpense.getId());
        Long expenseId = createdExpense.getId();

        // 2. Retrieve total for crop
        ResponseEntity<BigDecimal> totalResponse = restTemplate.exchange(
                "/api/expenses/crop/" + cropId + "/total",
                HttpMethod.GET,
                new HttpEntity<>(getHeaders()),
                BigDecimal.class);
        assertEquals(HttpStatus.OK, totalResponse.getStatusCode());
        assertEquals(0, new BigDecimal("500.00").compareTo(totalResponse.getBody()));

        // 3. Update the expense
        createdExpense.setAmount(new BigDecimal("600.00"));
        ResponseEntity<ExpenseDto> updateResponse = restTemplate.exchange(
                "/api/expenses/" + expenseId,
                HttpMethod.PUT,
                new HttpEntity<>(createdExpense, getHeaders()),
                ExpenseDto.class);
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals(0, new BigDecimal("600.00").compareTo(updateResponse.getBody().getAmount()));

        // 4. Delete the expense
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/expenses/" + expenseId,
                HttpMethod.DELETE,
                new HttpEntity<>(getHeaders()),
                Void.class);
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

        // Verify total is now 0 (or empty)
        ResponseEntity<BigDecimal> finalTotalResponse = restTemplate.exchange(
                "/api/expenses/crop/" + cropId + "/total",
                HttpMethod.GET,
                new HttpEntity<>(getHeaders()),
                BigDecimal.class);
        assertEquals(0, BigDecimal.ZERO.compareTo(finalTotalResponse.getBody()));
    }
}
