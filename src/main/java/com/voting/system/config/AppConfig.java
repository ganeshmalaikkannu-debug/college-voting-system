package com.voting.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Application Configuration.
 * 
 * Simple-English Explanation:
 * Defines core Spring beans.
 * BCryptPasswordEncoder uses a slow, salted cryptographic hashing algorithm
 * ensuring user passwords cannot be reversed even if the database is leaked.
 */
@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
