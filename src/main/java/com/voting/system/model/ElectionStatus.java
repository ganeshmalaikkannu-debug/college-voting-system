package com.voting.system.model;

/**
 * Election lifecycle status.
 * 
 * Simple-English Explanation:
 * The election moves strictly forward in one direction:
 * DRAFT -> UPCOMING -> ACTIVE -> CLOSED.
 * 
 * - DRAFT: Admin is setting up the title and candidates. Editable.
 * - UPCOMING: Finalized, waiting for the start date/time. Not open for voting yet.
 * - ACTIVE: Currently ongoing and open for voters to cast ballots within the time window.
 * - CLOSED: Voting has ended. Results can now be reviewed and published.
 */
public enum ElectionStatus {
    DRAFT,
    UPCOMING,
    ACTIVE,
    CLOSED;

    /**
     * Checks if transitioning from this status to nextStatus is allowed.
     * Only sequential forward transitions are valid.
     */
    public boolean canTransitionTo(ElectionStatus nextStatus) {
        if (this == DRAFT && nextStatus == UPCOMING) return true;
        if (this == UPCOMING && nextStatus == ACTIVE) return true;
        if (this == ACTIVE && nextStatus == CLOSED) return true;
        return false;
    }
}
