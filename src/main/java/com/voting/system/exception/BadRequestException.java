package com.voting.system.exception;

/**
 * Thrown on general invalid client requests, such as a candidate not belonging to an election.
 * Mapped to HTTP 400 BAD REQUEST.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
