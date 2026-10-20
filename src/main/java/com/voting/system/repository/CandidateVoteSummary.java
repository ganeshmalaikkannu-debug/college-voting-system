package com.voting.system.repository;

/**
 * Spring Data JPA Projection to hold aggregated vote counts per candidate.
 * 
 * Simple-English Explanation:
 * Spring Data automatically maps the SQL "GROUP BY candidate_id" aggregation
 * results directly into this interface.
 */
public interface CandidateVoteSummary {
    Long getCandidateId();
    Long getVoteCount();
}
