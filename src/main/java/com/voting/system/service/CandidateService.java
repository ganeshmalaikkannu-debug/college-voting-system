package com.voting.system.service;

import com.voting.system.dto.CandidateCreateRequest;
import com.voting.system.dto.CandidateResponse;
import com.voting.system.dto.CandidateUpdateRequest;
import com.voting.system.exception.BadRequestException;
import com.voting.system.exception.ResourceNotFoundException;
import com.voting.system.model.Candidate;
import com.voting.system.model.Election;
import com.voting.system.model.ElectionStatus;
import com.voting.system.repository.CandidateRepository;
import com.voting.system.repository.ElectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing candidates in an election.
 * 
 * Simple-English Explanation:
 * Handles nominations and candidate details.
 * Rule: Candidates can only be added, updated, or removed while the election is in DRAFT or UPCOMING.
 * Once voting is ACTIVE or CLOSED, the ballot lineup is permanently frozen!
 */
@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final ElectionRepository electionRepository;

    public CandidateService(CandidateRepository candidateRepository, ElectionRepository electionRepository) {
        this.candidateRepository = candidateRepository;
        this.electionRepository = electionRepository;
    }

    @Transactional
    public CandidateResponse addCandidate(Long electionId, CandidateCreateRequest request) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Election", "id", electionId));

        if (election.getStatus() == ElectionStatus.ACTIVE || election.getStatus() == ElectionStatus.CLOSED) {
            throw new BadRequestException("Cannot add candidates to an election that is " + election.getStatus());
        }

        Candidate candidate = new Candidate();
        candidate.setName(request.getName().trim());
        candidate.setManifesto(request.getManifesto().trim());
        candidate.setDepartment(request.getDepartment() != null ? request.getDepartment().trim() : null);
        candidate.setPosition(request.getPosition() != null ? request.getPosition().trim() : null);
        candidate.setPhotoUrl(request.getPhotoUrl() != null ? request.getPhotoUrl().trim() : null);
        candidate.setElection(election);

        Candidate saved = candidateRepository.save(candidate);
        return mapToResponse(saved);
    }

    @Transactional
    public CandidateResponse updateCandidate(Long candidateId, CandidateUpdateRequest request) {
        Candidate candidate = getCandidateEntity(candidateId);
        Election election = candidate.getElection();

        if (election.getStatus() == ElectionStatus.ACTIVE || election.getStatus() == ElectionStatus.CLOSED) {
            throw new BadRequestException("Cannot modify candidates in an election that is " + election.getStatus());
        }

        candidate.setName(request.getName().trim());
        candidate.setManifesto(request.getManifesto().trim());
        candidate.setDepartment(request.getDepartment() != null ? request.getDepartment().trim() : null);
        candidate.setPosition(request.getPosition() != null ? request.getPosition().trim() : null);
        candidate.setPhotoUrl(request.getPhotoUrl() != null ? request.getPhotoUrl().trim() : null);

        Candidate saved = candidateRepository.save(candidate);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteCandidate(Long candidateId) {
        Candidate candidate = getCandidateEntity(candidateId);
        Election election = candidate.getElection();

        if (election.getStatus() == ElectionStatus.ACTIVE || election.getStatus() == ElectionStatus.CLOSED) {
            throw new BadRequestException("Cannot remove candidates from an election that is " + election.getStatus());
        }

        candidateRepository.delete(candidate);
    }

    @Transactional(readOnly = true)
    public List<CandidateResponse> getCandidatesForElection(Long electionId) {
        if (!electionRepository.existsById(electionId)) {
            throw new ResourceNotFoundException("Election", "id", electionId);
        }
        return candidateRepository.findByElectionId(electionId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Candidate getCandidateEntity(Long candidateId) {
        return candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", candidateId));
    }

    private CandidateResponse mapToResponse(Candidate candidate) {
        return new CandidateResponse(
                candidate.getId(),
                candidate.getElection().getId(),
                candidate.getName(),
                candidate.getManifesto(),
                candidate.getDepartment(),
                candidate.getPosition(),
                candidate.getPhotoUrl()
        );
    }
}
