package com.voting.system.repository;

import com.voting.system.model.VoterParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for VoterParticipation.
 * 
 * Simple-English Explanation:
 * Allows checking if a voter has already participated in an election
 * and counting total voter turnout.
 */
@Repository
public interface VoterParticipationRepository extends JpaRepository<VoterParticipation, Long> {

    boolean existsByUserIdAndElectionId(Long userId, Long electionId);

    Optional<VoterParticipation> findByUserIdAndElectionId(Long userId, Long electionId);

    long countByElectionId(Long electionId);
}
