package com.voting.system.controller;

import com.voting.system.dto.VoteRequest;
import com.voting.system.dto.VoteResponse;
import com.voting.system.service.VotingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * REST Controller for casting votes.
 */
@RestController
@RequestMapping("/api/elections/{electionId}/votes")
public class VoteController {

    private final VotingService votingService;

    public VoteController(VotingService votingService) {
        this.votingService = votingService;
    }

    @PostMapping
    public ResponseEntity<VoteResponse> castVote(@PathVariable Long electionId,
                                                 @Valid @RequestBody VoteRequest request,
                                                 Principal principal) {
        if (principal == null) {
            throw new BadCredentialsException("Authentication required to cast a vote");
        }
        VoteResponse response = votingService.castVote(electionId, request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
