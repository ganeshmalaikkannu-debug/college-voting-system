package com.voting.system.controller;

import com.voting.system.dto.ElectionCreateRequest;
import com.voting.system.dto.ElectionResponse;
import com.voting.system.dto.ElectionResultResponse;
import com.voting.system.dto.ElectionStatusUpdateRequest;
import com.voting.system.dto.ElectionUpdateRequest;
import com.voting.system.dto.ParticipationStatsResponse;
import com.voting.system.service.ElectionService;
import com.voting.system.service.VotingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Elections.
 * 
 * Simple-English Explanation:
 * Provides CRUD endpoints for elections, lifecycle status changes,
 * and participation/results endpoints.
 */
@RestController
@RequestMapping("/api/elections")
public class ElectionController {

    private final ElectionService electionService;
    private final VotingService votingService;

    public ElectionController(ElectionService electionService, VotingService votingService) {
        this.electionService = electionService;
        this.votingService = votingService;
    }

    @GetMapping
    public ResponseEntity<List<ElectionResponse>> getAllElections(Principal principal) {
        String rollNumber = principal != null ? principal.getName() : null;
        List<ElectionResponse> elections = electionService.getAllElections(rollNumber);
        return ResponseEntity.ok(elections);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ElectionResponse> getElectionById(@PathVariable Long id, Principal principal) {
        String rollNumber = principal != null ? principal.getName() : null;
        ElectionResponse election = electionService.getElectionById(id, rollNumber);
        return ResponseEntity.ok(election);
    }

    @PostMapping
    public ResponseEntity<ElectionResponse> createElection(@Valid @RequestBody ElectionCreateRequest request,
                                                           Principal principal) {
        String createdBy = principal != null ? principal.getName() : "ADMIN";
        ElectionResponse created = electionService.createElection(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ElectionResponse> updateElection(@PathVariable Long id,
                                                           @Valid @RequestBody ElectionUpdateRequest request) {
        ElectionResponse updated = electionService.updateElection(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteElection(@PathVariable Long id) {
        electionService.deleteElection(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ElectionResponse> updateElectionStatus(@PathVariable Long id,
                                                                 @Valid @RequestBody ElectionStatusUpdateRequest request) {
        ElectionResponse updated = electionService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/results-visibility")
    public ResponseEntity<ElectionResponse> updateResultsVisibility(@PathVariable Long id,
                                                                    @RequestBody Map<String, Boolean> body) {
        boolean published = body.getOrDefault("published", true);
        ElectionResponse updated = electionService.setResultsVisibility(id, published);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<ElectionResultResponse> getElectionResults(@PathVariable Long id,
                                                                     Principal principal) {
        String rollNumber = principal != null ? principal.getName() : "GUEST";
        ElectionResultResponse results = votingService.getElectionResults(id, rollNumber);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}/participation")
    public ResponseEntity<ParticipationStatsResponse> getParticipationStats(@PathVariable Long id) {
        ParticipationStatsResponse stats = votingService.getParticipationStats(id);
        return ResponseEntity.ok(stats);
    }
}
