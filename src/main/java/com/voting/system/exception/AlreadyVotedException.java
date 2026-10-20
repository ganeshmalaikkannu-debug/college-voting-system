package com.voting.system.exception;

/**
 * Thrown when a voter attempts to vote more than once in the same election.
 * Mapped to HTTP 409 CONFLICT.
 */
public class AlreadyVotedException extends RuntimeException {

    public AlreadyVotedException(String message) {
        super(message);
    }

    public AlreadyVotedException(Long userId, Long electionId) {
        super(String.format("User %d has already cast a vote in election %d", userId, electionId));
    }
}
