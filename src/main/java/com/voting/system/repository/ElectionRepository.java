package com.voting.system.repository;

import com.voting.system.model.Election;
import com.voting.system.model.ElectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for Election entity.
 * 
 * Simple-English Explanation:
 * Provides standard CRUD database operations for Elections, plus custom query
 * methods to list elections by their status or ordered chronologically.
 */
@Repository
public interface ElectionRepository extends JpaRepository<Election, Long> {

    List<Election> findAllByOrderByCreatedAtDesc();

    List<Election> findByStatus(ElectionStatus status);
}
