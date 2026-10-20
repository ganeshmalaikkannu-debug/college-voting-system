package com.voting.system.exception;

import com.voting.system.model.ElectionStatus;

/**
 * Thrown when an invalid election status transition is requested
 * (e.g., trying to move an election from CLOSED back to ACTIVE).
 * Mapped to HTTP 400 BAD REQUEST.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }

    public InvalidStatusTransitionException(ElectionStatus currentStatus, ElectionStatus targetStatus) {
        super(String.format("Invalid status transition from '%s' to '%s'. Allowed flow is DRAFT -> UPCOMING -> ACTIVE -> CLOSED.",
                currentStatus, targetStatus));
    }
}
