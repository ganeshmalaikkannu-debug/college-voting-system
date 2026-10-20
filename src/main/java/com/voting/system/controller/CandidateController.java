package com.voting.system.controller;

import com.voting.system.dto.CandidateCreateRequest;
import com.voting.system.dto.CandidateResponse;
import com.voting.system.dto.CandidateUpdateRequest;
import com.voting.system.service.CandidateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Candidate nominations and updates.
 */
@RestController
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @GetMapping("/api/elections/{electionId}/candidates")
    public ResponseEntity<List<CandidateResponse>> getCandidatesByElection(@PathVariable Long electionId) {
        List<CandidateResponse> candidates = candidateService.getCandidatesForElection(electionId);
        return ResponseEntity.ok(candidates);
    }

    @PostMapping("/api/elections/{electionId}/candidates")
    public ResponseEntity<CandidateResponse> addCandidate(@PathVariable Long electionId,
                                                          @Valid @RequestBody CandidateCreateRequest request) {
        CandidateResponse candidate = candidateService.addCandidate(electionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(candidate);
    }

    @PutMapping("/api/candidates/{id}")
    public ResponseEntity<CandidateResponse> updateCandidate(@PathVariable Long id,
                                                             @Valid @RequestBody CandidateUpdateRequest request) {
        CandidateResponse updated = candidateService.updateCandidate(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/api/candidates/{id}")
    public ResponseEntity<Void> deleteCandidate(@PathVariable Long id) {
        candidateService.deleteCandidate(id);
        return ResponseEntity.noContent().build();
    }
}
