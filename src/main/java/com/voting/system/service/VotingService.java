package com.voting.system.service;

import com.voting.system.dto.CandidateResultDto;
import com.voting.system.dto.ElectionResultResponse;
import com.voting.system.dto.ParticipationStatsResponse;
import com.voting.system.dto.VoteRequest;
import com.voting.system.dto.VoteResponse;
import com.voting.system.exception.AlreadyVotedException;
import com.voting.system.exception.BadRequestException;
import com.voting.system.exception.ElectionClosedException;
import com.voting.system.exception.ResourceNotFoundException;
import com.voting.system.exception.UnauthorizedActionException;
import com.voting.system.model.Ballot;
import com.voting.system.model.Candidate;
import com.voting.system.model.Election;
import com.voting.system.model.ElectionStatus;
import com.voting.system.model.Role;
import com.voting.system.model.User;
import com.voting.system.model.VoterParticipation;
import com.voting.system.repository.BallotRepository;
import com.voting.system.repository.CandidateRepository;
import com.voting.system.repository.CandidateVoteSummary;
import com.voting.system.repository.ElectionRepository;
import com.voting.system.repository.UserRepository;
import com.voting.system.repository.VoterParticipationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service orchestrating voting transactions, ballot privacy, and results calculation.
 * 
 * Simple-English Explanation:
 * This is the most critical service in the entire system:
 * 
 * 1. Double-Voting Prevention:
 *    - Application check: Queries voterParticipationRepository before inserting.
 *    - Database guarantee: A unique constraint on (user_id, election_id).
 *    - Concurrency catch: If two requests arrive at the same millisecond, the database
 *      throws DataIntegrityViolationException, which is caught and converted to 409 Conflict.
 * 
 * 2. Secret Ballot:
 *    - VoterParticipation records THAT the user voted.
 *    - Ballot records the candidate choice with NO user link.
 *    - Both are saved in a single @Transactional method. If either fails, everything rolls back.
 * 
 * 3. Results Embargo:
 *    - Results remain hidden until published by an Admin (unless requested by an Admin).
 */
@Service
public class VotingService {

    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final VoterParticipationRepository voterParticipationRepository;
    private final BallotRepository ballotRepository;
    private final UserRepository userRepository;

    public VotingService(ElectionRepository electionRepository,
                         CandidateRepository candidateRepository,
                         VoterParticipationRepository voterParticipationRepository,
                         BallotRepository ballotRepository,
                         UserRepository userRepository) {
        this.electionRepository = electionRepository;
        this.candidateRepository = candidateRepository;
        this.voterParticipationRepository = voterParticipationRepository;
        this.ballotRepository = ballotRepository;
        this.userRepository = userRepository;
    }

    /**
     * Casts a ballot in a single atomic database transaction.
     */
    @Transactional
    public VoteResponse castVote(Long electionId, VoteRequest request, String voterRollNumber) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Load the election. Throw 404 if missing.
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Election", "id", electionId));

        // 2. Check the election is ACTIVE and inside its time window. Otherwise throw ElectionClosedException.
        if (election.getStatus() != ElectionStatus.ACTIVE) {
            throw new ElectionClosedException("Voting is not allowed because election status is: " + election.getStatus());
        }
        if (now.isBefore(election.getStartTime())) {
            throw new ElectionClosedException("Voting has not yet started. Opens at: " + election.getStartTime());
        }
        if (now.isAfter(election.getEndTime())) {
            throw new ElectionClosedException("Voting has closed. Ended at: " + election.getEndTime());
        }

        // 3. Check the candidate belongs to this election. Otherwise throw 400.
        Candidate candidate = candidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", request.getCandidateId()));

        if (!candidate.getElection().getId().equals(election.getId())) {
            throw new BadRequestException("Candidate ID " + candidate.getId() + " does not belong to election " + election.getId());
        }

        // Load the voter user entity
        User voter = userRepository.findByRollNumber(voterRollNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User", "rollNumber", voterRollNumber));

        // 4. Check whether a participation record exists. If yes, throw AlreadyVotedException (409).
        if (voterParticipationRepository.existsByUserIdAndElectionId(voter.getId(), election.getId())) {
            throw new AlreadyVotedException(voter.getId(), election.getId());
        }

        try {
            // 5. Save VoterParticipation, then Ballot, in the same transaction.
            VoterParticipation participation = new VoterParticipation(voter, election, now);
            voterParticipationRepository.save(participation);

            // Save Ballot with NO user reference to preserve ballot secrecy
            Ballot ballot = new Ballot(election, candidate, now);
            ballotRepository.save(ballot);

            // Flush to ensure database unique constraints are triggered inside this try block
            voterParticipationRepository.flush();
            ballotRepository.flush();

            return new VoteResponse("Your vote has been cast and securely recorded.", election.getId(), now);

        } catch (DataIntegrityViolationException ex) {
            // 6. Catch DataIntegrityViolationException from the unique constraint and return 409.
            // This safely handles two simultaneous / concurrent vote requests.
            throw new AlreadyVotedException("A vote has already been recorded for this user in this election");
        }
    }

    /**
     * Calculates election results.
     * Hidden until published, unless requested by an ADMIN.
     */
    @Transactional(readOnly = true)
    public ElectionResultResponse getElectionResults(Long electionId, String requesterRollNumber) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Election", "id", electionId));

        User requester = userRepository.findByRollNumber(requesterRollNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User", "rollNumber", requesterRollNumber));

        // Embargo check: standard voters cannot see results until published
        if (!election.isResultsPublished() && requester.getRole() != Role.ADMIN) {
            throw new UnauthorizedActionException("Results for '" + election.getTitle() + "' have not been published yet.");
        }

        long totalVotes = ballotRepository.countByElectionId(electionId);
        List<Candidate> candidates = candidateRepository.findByElectionId(electionId);
        List<CandidateVoteSummary> voteSummaries = ballotRepository.countVotesGroupedByCandidate(electionId);

        // Map candidateId -> count
        Map<Long, Long> countsMap = new HashMap<>();
        for (CandidateVoteSummary summary : voteSummaries) {
            countsMap.put(summary.getCandidateId(), summary.getVoteCount());
        }

        List<CandidateResultDto> candidateResults = new ArrayList<>();
        for (Candidate c : candidates) {
            long count = countsMap.getOrDefault(c.getId(), 0L);
            double percentage = totalVotes > 0 ? ((double) count / totalVotes) * 100.0 : 0.0;
            // Round to 2 decimal places
            percentage = Math.round(percentage * 100.0) / 100.0;

            candidateResults.add(new CandidateResultDto(
                    c.getId(),
                    c.getName(),
                    c.getDepartment(),
                    c.getPosition(),
                    c.getPhotoUrl(),
                    count,
                    percentage
            ));
        }

        // Sort descending by vote count
        candidateResults.sort((a, b) -> Long.compare(b.getVoteCount(), a.getVoteCount()));

        return new ElectionResultResponse(
                election.getId(),
                election.getTitle(),
                totalVotes,
                election.isResultsPublished(),
                candidateResults
        );
    }

    /**
     * Participation metrics for administrators.
     * Counts only, never voter identities.
     */
    @Transactional(readOnly = true)
    public ParticipationStatsResponse getParticipationStats(Long electionId) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Election", "id", electionId));

        long totalVoters = userRepository.count(); // All registered accounts
        long votesCast = voterParticipationRepository.countByElectionId(electionId);
        double turnout = totalVoters > 0 ? ((double) votesCast / totalVoters) * 100.0 : 0.0;
        turnout = Math.round(turnout * 100.0) / 100.0;

        return new ParticipationStatsResponse(
                election.getId(),
                election.getTitle(),
                totalVoters,
                votesCast,
                turnout
        );
    }
}
