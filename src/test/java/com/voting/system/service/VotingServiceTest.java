package com.voting.system.service;

import com.voting.system.dto.ElectionResultResponse;
import com.voting.system.dto.ParticipationStatsResponse;
import com.voting.system.dto.VoteRequest;
import com.voting.system.dto.VoteResponse;
import com.voting.system.exception.AlreadyVotedException;
import com.voting.system.exception.BadRequestException;
import com.voting.system.exception.ElectionClosedException;
import com.voting.system.exception.UnauthorizedActionException;
import com.voting.system.model.Candidate;
import com.voting.system.model.Election;
import com.voting.system.model.ElectionStatus;
import com.voting.system.model.Role;
import com.voting.system.model.User;
import com.voting.system.repository.BallotRepository;
import com.voting.system.repository.CandidateRepository;
import com.voting.system.repository.CandidateVoteSummary;
import com.voting.system.repository.ElectionRepository;
import com.voting.system.repository.UserRepository;
import com.voting.system.repository.VoterParticipationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VotingService business logic and voting integrity.
 */
@ExtendWith(MockitoExtension.class)
class VotingServiceTest {

    @Mock
    private ElectionRepository electionRepository;
    @Mock
    private CandidateRepository candidateRepository;
    @Mock
    private VoterParticipationRepository voterParticipationRepository;
    @Mock
    private BallotRepository ballotRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VotingService votingService;

    private User testVoter;
    private User testAdmin;
    private Election activeElection;
    private Candidate candidate1;
    private Candidate candidateOtherElection;

    @BeforeEach
    void setUp() {
        testVoter = new User("VOTER01", "Alice Student", "alice@college.edu", "hashpass", Role.VOTER);
        testVoter.setId(10L);

        testAdmin = new User("ADM001", "Admin Staff", "admin@college.edu", "hashpass", Role.ADMIN);
        testAdmin.setId(1L);

        activeElection = new Election("Council 2026", "Annual Elections",
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(2), "admin");
        activeElection.setId(100L);
        activeElection.setStatus(ElectionStatus.ACTIVE);
        activeElection.setResultsPublished(false);

        candidate1 = new Candidate("Bob", "Better food", "CSE", "President", null, activeElection);
        candidate1.setId(501L);

        Election anotherElection = new Election("Club 2026", "Robotics Club",
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1), "admin");
        anotherElection.setId(200L);
        candidateOtherElection = new Candidate("Charlie", "Robots", "ECE", "President", null, anotherElection);
        candidateOtherElection.setId(502L);
    }

    @Test
    @DisplayName("Successfully cast vote when election is ACTIVE and voter has not voted")
    void castVote_Success() {
        when(electionRepository.findById(100L)).thenReturn(Optional.of(activeElection));
        when(candidateRepository.findById(501L)).thenReturn(Optional.of(candidate1));
        when(userRepository.findByRollNumber("VOTER01")).thenReturn(Optional.of(testVoter));
        when(voterParticipationRepository.existsByUserIdAndElectionId(10L, 100L)).thenReturn(false);

        VoteResponse response = votingService.castVote(100L, new VoteRequest(501L), "VOTER01");

        assertThat(response).isNotNull();
        assertThat(response.getElectionId()).isEqualTo(100L);
        verify(voterParticipationRepository, times(1)).save(any());
        verify(ballotRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Throws ElectionClosedException when election is in DRAFT or UPCOMING")
    void castVote_ElectionNotActive_ThrowsException() {
        activeElection.setStatus(ElectionStatus.UPCOMING);
        when(electionRepository.findById(100L)).thenReturn(Optional.of(activeElection));

        assertThatThrownBy(() -> votingService.castVote(100L, new VoteRequest(501L), "VOTER01"))
                .isInstanceOf(ElectionClosedException.class)
                .hasMessageContaining("UPCOMING");
    }

    @Test
    @DisplayName("Throws ElectionClosedException when voting time window has passed")
    void castVote_ElectionExpired_ThrowsException() {
        activeElection.setEndTime(LocalDateTime.now().minusMinutes(5));
        when(electionRepository.findById(100L)).thenReturn(Optional.of(activeElection));

        assertThatThrownBy(() -> votingService.castVote(100L, new VoteRequest(501L), "VOTER01"))
                .isInstanceOf(ElectionClosedException.class)
                .hasMessageContaining("Voting has closed");
    }

    @Test
    @DisplayName("Throws BadRequestException when candidate belongs to a different election")
    void castVote_CandidateFromOtherElection_ThrowsBadRequest() {
        when(electionRepository.findById(100L)).thenReturn(Optional.of(activeElection));
        when(candidateRepository.findById(502L)).thenReturn(Optional.of(candidateOtherElection));

        assertThatThrownBy(() -> votingService.castVote(100L, new VoteRequest(502L), "VOTER01"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong to election");
    }

    @Test
    @DisplayName("Throws AlreadyVotedException (409) when user has already voted")
    void castVote_DuplicateVote_ThrowsAlreadyVotedException() {
        when(electionRepository.findById(100L)).thenReturn(Optional.of(activeElection));
        when(candidateRepository.findById(501L)).thenReturn(Optional.of(candidate1));
        when(userRepository.findByRollNumber("VOTER01")).thenReturn(Optional.of(testVoter));
        when(voterParticipationRepository.existsByUserIdAndElectionId(10L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> votingService.castVote(100L, new VoteRequest(501L), "VOTER01"))
                .isInstanceOf(AlreadyVotedException.class);
    }

    @Test
    @DisplayName("Simultaneous vote triggers DataIntegrityViolationException and returns AlreadyVotedException")
    void castVote_ConcurrentRaceCondition_ThrowsAlreadyVotedException() {
        when(electionRepository.findById(100L)).thenReturn(Optional.of(activeElection));
        when(candidateRepository.findById(501L)).thenReturn(Optional.of(candidate1));
        when(userRepository.findByRollNumber("VOTER01")).thenReturn(Optional.of(testVoter));
        when(voterParticipationRepository.existsByUserIdAndElectionId(10L, 100L)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("Unique constraint uk_user_election_participation"))
                .when(voterParticipationRepository).flush();

        assertThatThrownBy(() -> votingService.castVote(100L, new VoteRequest(501L), "VOTER01"))
                .isInstanceOf(AlreadyVotedException.class)
                .hasMessageContaining("already been recorded");
    }

    @Test
    @DisplayName("Standard voter cannot see results if unpublished (Throws UnauthorizedActionException)")
    void getElectionResults_UnpublishedForVoter_ThrowsUnauthorized() {
        activeElection.setResultsPublished(false);
        when(electionRepository.findById(100L)).thenReturn(Optional.of(activeElection));
        when(userRepository.findByRollNumber("VOTER01")).thenReturn(Optional.of(testVoter));

        assertThatThrownBy(() -> votingService.getElectionResults(100L, "VOTER01"))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("not been published yet");
    }

    @Test
    @DisplayName("Admin can view unpublished results")
    void getElectionResults_UnpublishedForAdmin_Allowed() {
        activeElection.setResultsPublished(false);
        when(electionRepository.findById(100L)).thenReturn(Optional.of(activeElection));
        when(userRepository.findByRollNumber("ADM001")).thenReturn(Optional.of(testAdmin));
        when(ballotRepository.countByElectionId(100L)).thenReturn(10L);
        when(candidateRepository.findByElectionId(100L)).thenReturn(List.of(candidate1));

        CandidateVoteSummary summary = mock(CandidateVoteSummary.class);
        when(summary.getCandidateId()).thenReturn(501L);
        when(summary.getVoteCount()).thenReturn(10L);
        when(ballotRepository.countVotesGroupedByCandidate(100L)).thenReturn(List.of(summary));

        ElectionResultResponse results = votingService.getElectionResults(100L, "ADM001");
        assertThat(results).isNotNull();
        assertThat(results.getTotalVotesCast()).isEqualTo(10L);
        assertThat(results.getCandidateResults().get(0).getPercentage()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Participation stats returns correct turnout without exposing individual identities")
    void getParticipationStats_Success() {
        when(electionRepository.findById(100L)).thenReturn(Optional.of(activeElection));
        when(userRepository.count()).thenReturn(50L);
        when(voterParticipationRepository.countByElectionId(100L)).thenReturn(25L);

        ParticipationStatsResponse stats = votingService.getParticipationStats(100L);
        assertThat(stats.getTotalRegisteredVoters()).isEqualTo(50L);
        assertThat(stats.getVotesCast()).isEqualTo(25L);
        assertThat(stats.getTurnoutPercentage()).isEqualTo(50.0);
    }
}
