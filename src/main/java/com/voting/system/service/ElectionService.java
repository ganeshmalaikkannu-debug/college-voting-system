package com.voting.system.service;

import com.voting.system.dto.ElectionCreateRequest;
import com.voting.system.dto.ElectionResponse;
import com.voting.system.dto.ElectionUpdateRequest;
import com.voting.system.exception.BadRequestException;
import com.voting.system.exception.InvalidStatusTransitionException;
import com.voting.system.exception.ResourceNotFoundException;
import com.voting.system.model.Election;
import com.voting.system.model.ElectionStatus;
import com.voting.system.model.Role;
import com.voting.system.model.User;
import com.voting.system.repository.CandidateRepository;
import com.voting.system.repository.ElectionRepository;
import com.voting.system.repository.UserRepository;
import com.voting.system.repository.VoterParticipationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing the Election lifecycle and administrative updates.
 * 
 * Simple-English Explanation:
 * Controls the flow of elections:
 * - Creating and editing is allowed only in DRAFT status.
 * - Changing status follows strict rules (DRAFT -> UPCOMING -> ACTIVE -> CLOSED).
 * - Enforces that voting start time must be before end time.
 */
@Service
public class ElectionService {

    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final VoterParticipationRepository voterParticipationRepository;
    private final UserRepository userRepository;

    public ElectionService(ElectionRepository electionRepository,
                           CandidateRepository candidateRepository,
                           VoterParticipationRepository voterParticipationRepository,
                           UserRepository userRepository) {
        this.electionRepository = electionRepository;
        this.candidateRepository = candidateRepository;
        this.voterParticipationRepository = voterParticipationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ElectionResponse createElection(ElectionCreateRequest request, String createdBy) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("Election end time must be after start time");
        }

        Election election = new Election();
        election.setTitle(request.getTitle().trim());
        election.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        election.setStartTime(request.getStartTime());
        election.setEndTime(request.getEndTime());
        election.setStatus(ElectionStatus.DRAFT);
        election.setResultsPublished(false);
        election.setCreatedBy(createdBy);

        Election saved = electionRepository.save(election);
        return mapToResponse(saved, null);
    }

    @Transactional
    public ElectionResponse updateElection(Long id, ElectionUpdateRequest request) {
        Election election = getElectionEntity(id);

        if (election.getStatus() != ElectionStatus.DRAFT) {
            throw new BadRequestException("Only elections in DRAFT status can be modified. Current status: " + election.getStatus());
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("Election end time must be after start time");
        }

        election.setTitle(request.getTitle().trim());
        election.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        election.setStartTime(request.getStartTime());
        election.setEndTime(request.getEndTime());

        Election saved = electionRepository.save(election);
        return mapToResponse(saved, null);
    }

    @Transactional
    public void deleteElection(Long id) {
        Election election = getElectionEntity(id);

        if (election.getStatus() != ElectionStatus.DRAFT) {
            throw new BadRequestException("Only elections in DRAFT status can be deleted. Current status: " + election.getStatus());
        }

        electionRepository.delete(election);
    }

    @Transactional
    public ElectionResponse updateStatus(Long id, ElectionStatus targetStatus) {
        Election election = getElectionEntity(id);
        ElectionStatus currentStatus = election.getStatus();

        if (currentStatus == targetStatus) {
            return mapToResponse(election, null);
        }

        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, targetStatus);
        }

        election.setStatus(targetStatus);
        Election saved = electionRepository.save(election);
        return mapToResponse(saved, null);
    }

    @Transactional
    public ElectionResponse setResultsVisibility(Long id, boolean published) {
        Election election = getElectionEntity(id);
        election.setResultsPublished(published);
        Election saved = electionRepository.save(election);
        return mapToResponse(saved, null);
    }

    @Transactional(readOnly = true)
    public ElectionResponse getElectionById(Long id, String currentUserRoll) {
        Election election = getElectionEntity(id);
        return mapToResponse(election, currentUserRoll);
    }

    @Transactional(readOnly = true)
    public List<ElectionResponse> getAllElections(String currentUserRoll) {
        List<Election> elections = electionRepository.findAllByOrderByCreatedAtDesc();
        return elections.stream()
                .map(e -> mapToResponse(e, currentUserRoll))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Election getElectionEntity(Long id) {
        return electionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Election", "id", id));
    }

    private ElectionResponse mapToResponse(Election election, String currentUserRoll) {
        int candidateCount = (int) candidateRepository.countByElectionId(election.getId());

        Boolean hasVoted = null;
        if (currentUserRoll != null) {
            userRepository.findByRollNumber(currentUserRoll).ifPresent(user -> {
                if (user.getRole() == Role.VOTER) {
                    boolean participated = voterParticipationRepository.existsByUserIdAndElectionId(user.getId(), election.getId());
                    // We set hasVoted in response
                }
            });
            User user = userRepository.findByRollNumber(currentUserRoll).orElse(null);
            if (user != null && user.getRole() == Role.VOTER) {
                hasVoted = voterParticipationRepository.existsByUserIdAndElectionId(user.getId(), election.getId());
            }
        }

        return new ElectionResponse(
                election.getId(),
                election.getTitle(),
                election.getDescription(),
                election.getStatus(),
                election.getStartTime(),
                election.getEndTime(),
                election.isResultsPublished(),
                election.getCreatedBy(),
                election.getCreatedAt(),
                election.getUpdatedAt(),
                candidateCount,
                hasVoted
        );
    }
}
