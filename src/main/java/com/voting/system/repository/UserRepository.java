package com.voting.system.repository;

import com.voting.system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for User entity.
 * 
 * Simple-English Explanation:
 * Spring Data automatically generates the SQL queries for these methods
 * based on the method names (e.g., findByRollNumber generates "SELECT * FROM users WHERE roll_number = ?").
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByRollNumber(String rollNumber);

    Optional<User> findByEmail(String email);

    boolean existsByRollNumber(String rollNumber);

    boolean existsByEmail(String email);
}
