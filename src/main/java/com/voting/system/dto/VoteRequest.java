package com.voting.system.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for casting a vote in an election.
 */
public class VoteRequest {

    @NotNull(message = "Candidate ID is required")
    private Long candidateId;

    public VoteRequest() {
    }

    public VoteRequest(Long candidateId) {
        this.candidateId = candidateId;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }
}
