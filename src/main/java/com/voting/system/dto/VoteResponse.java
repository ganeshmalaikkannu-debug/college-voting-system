package com.voting.system.dto;

import java.time.LocalDateTime;

/**
 * Neutral vote confirmation response.
 * 
 * Simple-English Explanation:
 * Confirms receipt and persistence of the ballot without broadcasting
 * candidate choice.
 */
public class VoteResponse {

    private String message;
    private Long electionId;
    private LocalDateTime castTime;

    public VoteResponse() {
    }

    public VoteResponse(String message, Long electionId, LocalDateTime castTime) {
        this.message = message;
        this.electionId = electionId;
        this.castTime = castTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getElectionId() {
        return electionId;
    }

    public void setElectionId(Long electionId) {
        this.electionId = electionId;
    }

    public LocalDateTime getCastTime() {
        return castTime;
    }

    public void setCastTime(LocalDateTime castTime) {
        this.castTime = castTime;
    }
}
