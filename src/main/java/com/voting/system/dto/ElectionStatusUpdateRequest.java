package com.voting.system.dto;

import com.voting.system.model.ElectionStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for transitioning the election lifecycle state.
 */
public class ElectionStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private ElectionStatus status;

    public ElectionStatusUpdateRequest() {
    }

    public ElectionStatusUpdateRequest(ElectionStatus status) {
        this.status = status;
    }

    public ElectionStatus getStatus() {
        return status;
    }

    public void setStatus(ElectionStatus status) {
        this.status = status;
    }
}
