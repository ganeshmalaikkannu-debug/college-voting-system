package com.voting.system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic Spring Boot context sanity test.
 * 
 * Simple-English Explanation:
 * Tests that Spring Boot can start up completely, configure all JPA entities,
 * and create the database schema in the in-memory H2 test database without any errors.
 */
@SpringBootTest
@ActiveProfiles("test")
class VotingSystemApplicationTests {

    @Test
    void contextLoads() {
        // If the application context starts without throwing an exception, this test passes.
    }
}
