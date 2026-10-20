package com.voting.system.repository;

import com.voting.system.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Candidate entity.
 * 
 * Simple-English Explanation:
 * Handles database operations for candidates, such as retrieving all candidates
 * nominated in a specific election or checking if candidates exist.
 */
@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    List<Candidate> findByElectionId(Long electionId);

    Optional<Candidate> findByIdAndElectionId(Long id, Long electionId);

    boolean existsByElectionId(Long electionId);

    long countByElectionId(Long electionId);
}
