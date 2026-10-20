package com.voting.system.exception;

/**
 * Thrown when an attempt is made to vote in an election that is not currently ACTIVE,
 * or whose time window has expired or not yet begun.
 * Mapped to HTTP 400 BAD REQUEST.
 */
public class ElectionClosedException extends RuntimeException {

    public ElectionClosedException(String message) {
        super(message);
    }
}
