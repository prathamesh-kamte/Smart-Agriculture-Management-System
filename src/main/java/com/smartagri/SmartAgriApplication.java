package com.smartagri;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Smart Agriculture Management System.
 * Enables scheduling for automated crop advisory and irrigation tasks.
 */
@SpringBootApplication
@EnableScheduling
public class SmartAgriApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartAgriApplication.class, args);
    }
}
