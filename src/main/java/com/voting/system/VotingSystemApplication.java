package com.voting.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the College Voting System application.
 * 
 * Simple-English Explanation:
 * The @SpringBootApplication annotation starts the entire Spring Boot framework,
 * automatically registers components (controllers, services, repositories) via component scanning,
 * and configures the embedded Tomcat web server.
 */
@SpringBootApplication
public class VotingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(VotingSystemApplication.class, args);
    }
}
