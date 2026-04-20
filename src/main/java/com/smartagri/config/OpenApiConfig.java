package com.smartagri.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the SpringDoc OpenAPI 3.0 specification.
 *
 * <p>Exposes a global "bearerAuth" security scheme so that the Swagger UI
 * allows authenticated requests to protected endpoints directly from the browser.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI smartAgriOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, jwtSecurityScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("Smart Agriculture Management System API")
                .description("""
                        Backend REST API for the Smart Agriculture Management System.
                        
                        Features:
                        - JWT-secured authentication with role-based access control
                        - Full crop lifecycle management (CRUD + status transitions)
                        - Granular expense tracking per crop
                        - Rule-based agricultural advisory engine
                        - Automated scheduled advisory generation
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Smart Agri Team")
                        .email("support@smartagri.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Provide the JWT obtained from POST /api/auth/login");
    }
}
