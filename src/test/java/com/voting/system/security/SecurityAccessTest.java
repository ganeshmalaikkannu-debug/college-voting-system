package com.voting.system.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voting.system.dto.ElectionCreateRequest;
import com.voting.system.dto.VoteRequest;
import com.voting.system.model.Election;
import com.voting.system.model.ElectionStatus;
import com.voting.system.model.Role;
import com.voting.system.model.User;
import com.voting.system.repository.CandidateRepository;
import com.voting.system.repository.ElectionRepository;
import com.voting.system.repository.UserRepository;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration security tests verifying Spring Security RBAC and JWT validation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private com.voting.system.repository.BallotRepository ballotRepository;

    @Autowired
    private com.voting.system.repository.VoterParticipationRepository voterParticipationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String voterToken;
    private Long testElectionId;

    @BeforeEach
    void setUp() {
        ballotRepository.deleteAll();
        voterParticipationRepository.deleteAll();
        candidateRepository.deleteAll();
        electionRepository.deleteAll();
        userRepository.deleteAll();

        // Create Admin
        User admin = new User("ADM001", "Admin Staff", "admin@college.edu", passwordEncoder.encode("Pass@123"), Role.ADMIN);
        userRepository.save(admin);
        adminToken = jwtTokenProvider.generateToken("ADM001", "ADMIN", "Admin Staff");

        // Create Voter
        User voter = new User("VOTER01", "Alice Student", "alice@college.edu", passwordEncoder.encode("Pass@123"), Role.VOTER);
        userRepository.save(voter);
        voterToken = jwtTokenProvider.generateToken("VOTER01", "VOTER", "Alice Student");

        // Create Election
        Election election = new Election("Council 2026", "Test", LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(1), "ADM001");
        election.setStatus(ElectionStatus.ACTIVE);
        electionRepository.save(election);
        testElectionId = election.getId();
    }

    @Test
    @DisplayName("Unauthenticated request to /api/elections returns HTTP 401 Unauthorized")
    void accessWithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/elections"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("VOTER token can view /api/elections (HTTP 200 OK)")
    void voterAccess_GetElections_Returns200() throws Exception {
        mockMvc.perform(get("/api/elections")
                        .header("Authorization", "Bearer " + voterToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("VOTER token attempting to create election returns HTTP 403 Forbidden")
    void voterCreateElection_Forbidden403() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        ElectionCreateRequest request = new ElectionCreateRequest("Illegal", "Desc", now.plusHours(1), now.plusHours(5));

        mockMvc.perform(post("/api/elections")
                        .header("Authorization", "Bearer " + voterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("VOTER token attempting to access participation stats returns HTTP 403 Forbidden")
    void voterAccessParticipation_Forbidden403() throws Exception {
        mockMvc.perform(get("/api/elections/" + testElectionId + "/participation")
                        .header("Authorization", "Bearer " + voterToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("ADMIN token can access participation stats (HTTP 200 OK)")
    void adminAccessParticipation_Returns200() throws Exception {
        mockMvc.perform(get("/api/elections/" + testElectionId + "/participation")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.electionId").value(testElectionId));
    }

    @Test
    @DisplayName("ADMIN token can create election (HTTP 201 Created)")
    void adminCreateElection_Returns201() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        ElectionCreateRequest request = new ElectionCreateRequest("Admin Created", "Desc", now.plusHours(1), now.plusHours(5));

        mockMvc.perform(post("/api/elections")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Admin Created"));
    }

    @Test
    @DisplayName("ADMIN token attempting to vote returns HTTP 403 Forbidden (Only VOTER can vote)")
    void adminCastVote_Forbidden403() throws Exception {
        VoteRequest req = new VoteRequest(1L);

        mockMvc.perform(post("/api/elections/" + testElectionId + "/votes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
