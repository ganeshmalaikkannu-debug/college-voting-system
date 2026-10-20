package com.voting.system.integration;

import com.voting.system.dto.VoteRequest;
import com.voting.system.exception.AlreadyVotedException;
import com.voting.system.exception.UnauthorizedActionException;
import com.voting.system.model.*;
import com.voting.system.repository.*;
import com.voting.system.service.VotingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * High-concurrency integration test validating:
 * 1. Concurrency Race Conditions: Two simultaneous threads casting a ballot for the exact same voter.
 *    The DB unique constraint (user_id, election_id) guarantees that exactly 1 vote succeeds
 *    and the second thread is rejected with AlreadyVotedException / DataIntegrityViolationException.
 * 2. Transaction Rollback: Verifies zero partial writes when an operation aborts.
 * 3. Results Embargo: Verifies strict programmatic guards preventing tally access before publication.
 */
@SpringBootTest
@ActiveProfiles("test")
public class ConcurrencyAndEmbargoIntegrationTest {

    @Autowired
    private VotingService votingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private VoterParticipationRepository voterParticipationRepository;

    @Autowired
    private BallotRepository ballotRepository;

    private User testVoter;
    private Election activeElection;
    private Candidate candidateA;
    private Candidate candidateB;

    @BeforeEach
    void setUp() {
        ballotRepository.deleteAll();
        voterParticipationRepository.deleteAll();
        candidateRepository.deleteAll();
        electionRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create a Voter
        testVoter = new User("24CS888", "Concurrent Voter", "voter888@apex.edu", "TestPassword@123", Role.VOTER);
        userRepository.save(testVoter);

        // 2. Create an Active Election
        activeElection = new Election(
                "Concurrent Test Election",
                "Verifying thread safety and atomic transaction boundaries",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(2),
                "ADM001"
        );
        activeElection.setStatus(ElectionStatus.ACTIVE);
        activeElection.setResultsPublished(false);
        electionRepository.save(activeElection);

        // 3. Create Candidates
        candidateA = new Candidate("Candidate Alpha", "Alpha Platform", "CS", "Rep", "https://photo", activeElection);
        candidateRepository.save(candidateA);

        candidateB = new Candidate("Candidate Beta", "Beta Platform", "CS", "Rep", "https://photo", activeElection);
        candidateRepository.save(candidateB);
    }

    @Test
    @DisplayName("Concurrency: Simultaneous vote requests for same voter - exactly 1 succeeds, 1 rejected")
    void simultaneousVoteRequests_raceConditionPrevented() throws Exception {
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final Long candidateId = (i == 0) ? candidateA.getId() : candidateB.getId();
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    // All threads wait here until released simultaneously
                    startLatch.await();
                    votingService.castVote(activeElection.getId(), new VoteRequest(candidateId), testVoter.getRollNumber());
                    successCount.incrementAndGet();
                } catch (AlreadyVotedException | DataIntegrityViolationException ex) {
                    conflictCount.incrementAndGet();
                } catch (Exception ex) {
                    // Unexpected exception
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        // Release both threads at the exact same instant
        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        // Exactly one thread must have succeeded and one thread must have encountered a conflict
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        // Verify Database Integrity: Exactly 1 VoterParticipation and 1 Ballot
        assertThat(voterParticipationRepository.count()).isEqualTo(1);
        assertThat(ballotRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Transaction Rollback: Rejection rolls back participation and leaves zero orphan ballots")
    void transactionRollback_noOrphanRecords() {
        // First vote succeeds
        votingService.castVote(activeElection.getId(), new VoteRequest(candidateA.getId()), testVoter.getRollNumber());
        assertThat(voterParticipationRepository.count()).isEqualTo(1);
        assertThat(ballotRepository.count()).isEqualTo(1);

        // Second vote must throw AlreadyVotedException and rollback completely
        assertThatThrownBy(() -> votingService.castVote(
                activeElection.getId(), new VoteRequest(candidateB.getId()), testVoter.getRollNumber()))
                .isInstanceOf(AlreadyVotedException.class);

        // Counts must strictly remain 1 and 1
        assertThat(voterParticipationRepository.count()).isEqualTo(1);
        assertThat(ballotRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Results Embargo: Direct service-layer verification of active and unpublished embargo guards")
    void resultsEmbargo_guardsEnforced() {
        // 1. Guard against tally calculation while results are unpublished for VOTER
        assertThatThrownBy(() -> votingService.getElectionResults(activeElection.getId(), testVoter.getRollNumber()))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("not been published yet");

        // 2. Transition to CLOSED, but resultsPublished is still FALSE
        activeElection.setStatus(ElectionStatus.CLOSED);
        activeElection.setResultsPublished(false);
        electionRepository.save(activeElection);

        // Voter accessing embargoed results must still be denied with UnauthorizedActionException
        assertThatThrownBy(() -> votingService.getElectionResults(activeElection.getId(), testVoter.getRollNumber()))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("not been published yet");

        // 3. Publish results
        activeElection.setResultsPublished(true);
        electionRepository.save(activeElection);

        // Voter can now successfully retrieve published results
        var results = votingService.getElectionResults(activeElection.getId(), testVoter.getRollNumber());
        assertThat(results).isNotNull();
        assertThat(results.getElectionId()).isEqualTo(activeElection.getId());
        assertThat(results.getTotalVotesCast()).isEqualTo(0);
    }
}
