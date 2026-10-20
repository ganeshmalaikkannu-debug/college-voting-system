package com.voting.system.exception;

/**
 * Thrown when an authenticated user attempts an operation they are not allowed to perform
 * (e.g., viewing unpublished results as a standard voter).
 * Mapped to HTTP 403 FORBIDDEN.
 */
public class UnauthorizedActionException extends RuntimeException {

    public UnauthorizedActionException(String message) {
        super(message);
    }
}
