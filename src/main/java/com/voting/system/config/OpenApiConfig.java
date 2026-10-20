package com.voting.system.config;

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
 * SpringDoc OpenAPI 3.0 Configuration.
 * 
 * Provides interactive API documentation at /swagger-ui.html with
 * JWT Bearer token authentication support for testing secured endpoints directly in the browser.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("College Voting System REST API")
                        .version("1.0.0")
                        .description("Audit-compliant, privacy-preserving institutional voting web application. " +
                                "Implements separate VoterParticipation and anonymous Ballot tables, " +
                                "Role-Based Access Control (RBAC), and embargoed results calculation.")
                        .contact(new Contact()
                                .name("Ganesh - Project Lead")
                                .email("ganesh.project@apex.edu"))
                        .license(new License()
                                .name("Academic Free License")
                                .url("https://apex.edu/license")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT token obtained from /api/auth/login or /api/auth/register")));
    }
}
