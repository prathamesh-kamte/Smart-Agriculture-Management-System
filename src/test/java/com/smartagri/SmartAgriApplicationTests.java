package com.smartagri;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test – verifies that the Spring application context loads without errors.
 */
@SpringBootTest
@ActiveProfiles("dev")
class SmartAgriApplicationTests {

    @Test
    void contextLoads() {
        // Passes if the application context starts successfully.
    }
}
