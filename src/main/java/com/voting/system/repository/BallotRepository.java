package com.voting.system.repository;

import com.voting.system.model.Ballot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for Ballot entity.
 * 
 * Simple-English Explanation:
 * Securely counts ballots without knowing who cast them.
 * The custom JPQL query groups ballots by candidate to compute real-time
 * election standings.
 */
@Repository
public interface BallotRepository extends JpaRepository<Ballot, Long> {

    long countByElectionId(Long electionId);

    long countByElectionIdAndCandidateId(Long electionId, Long candidateId);

    @Query("SELECT b.candidate.id AS candidateId, COUNT(b) AS voteCount " +
           "FROM Ballot b WHERE b.election.id = :electionId " +
           "GROUP BY b.candidate.id")
    List<CandidateVoteSummary> countVotesGroupedByCandidate(@Param("electionId") Long electionId);
}
