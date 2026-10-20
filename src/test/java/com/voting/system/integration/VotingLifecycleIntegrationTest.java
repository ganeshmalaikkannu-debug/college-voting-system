package com.voting.system.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voting.system.dto.*;
import com.voting.system.model.ElectionStatus;
import com.voting.system.model.Role;
import com.voting.system.model.User;
import com.voting.system.repository.*;
import com.voting.system.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End Integration Test validating the full institutional voting lifecycle:
 * 1. Admin creates election
 * 2. Admin nominates candidates
 * 3. Lifecycle transitions (DRAFT -> UPCOMING -> ACTIVE)
 * 4. Voter registration and authentication
 * 5. Ballot cast with anonymous voter participation
 * 6. Concurrency & double-vote rejection (HTTP 409 Conflict)
 * 7. Participation turnout audit (voter identity confirmed without ballot disclosure)
 * 8. Status transition to CLOSED and Results Embargo enforcement
 * 9. Results publication and certified tally calculation
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class VotingLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;

    @BeforeEach
    void setUp() {
        ballotRepository.deleteAll();
        voterParticipationRepository.deleteAll();
        candidateRepository.deleteAll();
        electionRepository.deleteAll();
        userRepository.deleteAll();

        // Seed Admin user and generate JWT
        User admin = new User("ADM999", "System Admin", "admin.audit@apex.edu", passwordEncoder.encode("AdminPass@123"), Role.ADMIN);
        userRepository.save(admin);
        adminToken = "Bearer " + jwtTokenProvider.generateToken(admin.getRollNumber(), admin.getRole().name(), admin.getName());
    }

    @Test
    @DisplayName("End-to-End Voting Lifecycle: Creation, Nomination, Voting, Audit, Embargo & Publication")
    void fullVotingLifecycle() throws Exception {
        // Step 1: Admin creates Election (DRAFT)
        ElectionCreateRequest createRequest = new ElectionCreateRequest(
                "2026 Sports Secretary Election",
                "Annual election for Sports Secretary",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(5)
        );

        MvcResult createElectionResult = mockMvc.perform(post("/api/elections")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        ElectionResponse electionResponse = objectMapper.readValue(
                createElectionResult.getResponse().getContentAsString(), ElectionResponse.class);
        Long electionId = electionResponse.getId();

        // Step 2: Admin nominates 2 Candidates
        CandidateCreateRequest c1Request = new CandidateCreateRequest(
                "Vikram Singh",
                "Improve college sports equipment and grounds",
                "Mechanical Engineering",
                "Sports Secretary",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb"
        );

        MvcResult c1Result = mockMvc.perform(post("/api/elections/" + electionId + "/candidates")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c1Request)))
                .andExpect(status().isCreated())
                .andReturn();
        CandidateResponse candidate1 = objectMapper.readValue(c1Result.getResponse().getContentAsString(), CandidateResponse.class);

        CandidateCreateRequest c2Request = new CandidateCreateRequest(
                "Ananya Gupta",
                "Organize inter-collegiate athletic tournaments",
                "Computer Science",
                "Sports Secretary",
                "https://images.unsplash.com/photo-1517841905240-472988babdf9"
        );

        MvcResult c2Result = mockMvc.perform(post("/api/elections/" + electionId + "/candidates")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c2Request)))
                .andExpect(status().isCreated())
                .andReturn();
        CandidateResponse candidate2 = objectMapper.readValue(c2Result.getResponse().getContentAsString(), CandidateResponse.class);

        // Step 3: Transition election: DRAFT -> UPCOMING -> ACTIVE
        mockMvc.perform(patch("/api/elections/" + electionId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ElectionStatusUpdateRequest(ElectionStatus.UPCOMING))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPCOMING"));

        mockMvc.perform(patch("/api/elections/" + electionId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ElectionStatusUpdateRequest(ElectionStatus.ACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Step 4: Voter registers and signs in
        RegisterRequest registerRequest = new RegisterRequest("24ME055", "Rahul Verma", "rahul.v@apex.edu", "RahulSecure@123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rollNumber").value("24ME055"))
                .andExpect(jsonPath("$.role").value("VOTER"));

        LoginRequest loginRequest = new LoginRequest("24ME055", "RahulSecure@123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);
        String voterToken = "Bearer " + authResponse.getToken();

        // Step 5: Results Embargo Check while ACTIVE and results unpublished
        mockMvc.perform(get("/api/elections/" + electionId + "/results")
                        .header("Authorization", voterToken))
                .andExpect(status().isForbidden());

        // Step 6: Voter casts ballot for Candidate 1
        VoteRequest voteRequest = new VoteRequest(candidate1.getId());
        mockMvc.perform(post("/api/elections/" + electionId + "/votes")
                        .header("Authorization", voterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.electionId").value(electionId))
                .andExpect(jsonPath("$.castTime").isNotEmpty());

        // Verify Database state: 1 Participation record and 1 Anonymous Ballot record
        assertThat(voterParticipationRepository.count()).isEqualTo(1);
        assertThat(ballotRepository.count()).isEqualTo(1);

        // Step 7: Double Voting Prevention
        // Voter attempts to cast another vote for candidate 2 -> must return 409 Conflict
        VoteRequest secondVoteRequest = new VoteRequest(candidate2.getId());
        mockMvc.perform(post("/api/elections/" + electionId + "/votes")
                        .header("Authorization", voterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondVoteRequest)))
                .andExpect(status().isConflict());

        // Ensure database state was not altered
        assertThat(ballotRepository.count()).isEqualTo(1);

        // Step 8: Participation Audit Turnout Inspection by Admin
        // Admin verifies participation (turnout aggregates)
        // CRITICAL PRIVACY RULE: Never discloses candidate choice or who voted for whom.
        mockMvc.perform(get("/api/elections/" + electionId + "/participation")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votesCast").value(1))
                .andExpect(jsonPath("$.totalRegisteredVoters").value(2))
                .andExpect(jsonPath("$.turnoutPercentage").value(50.0));

        // Step 9: Close Election
        mockMvc.perform(patch("/api/elections/" + electionId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ElectionStatusUpdateRequest(ElectionStatus.CLOSED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        // Results are CLOSED but resultsPublished is still FALSE (Embargoed) -> 403 Forbidden
        mockMvc.perform(get("/api/elections/" + electionId + "/results")
                        .header("Authorization", voterToken))
                .andExpect(status().isForbidden());

        // Step 10: Admin Publishes Results
        mockMvc.perform(patch("/api/elections/" + electionId + "/results-visibility")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("published", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultsPublished").value(true));

        // Step 11: Public Results Verification
        mockMvc.perform(get("/api/elections/" + electionId + "/results")
                        .header("Authorization", voterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.electionId").value(electionId))
                .andExpect(jsonPath("$.totalVotesCast").value(1))
                .andExpect(jsonPath("$.candidateResults[0].candidateId").value(candidate1.getId()))
                .andExpect(jsonPath("$.candidateResults[0].voteCount").value(1))
                .andExpect(jsonPath("$.candidateResults[0].percentage").value(100.0));
    }
}
