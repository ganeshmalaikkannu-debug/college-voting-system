package com.voting.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voting.system.dto.CandidateCreateRequest;
import com.voting.system.dto.CandidateResponse;
import com.voting.system.dto.VoteRequest;
import com.voting.system.dto.VoteResponse;
import com.voting.system.exception.AlreadyVotedException;
import com.voting.system.exception.GlobalExceptionHandler;
import com.voting.system.service.CandidateService;
import com.voting.system.service.VotingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for CandidateController and VoteController.
 */
@WebMvcTest(controllers = {CandidateController.class, VoteController.class}, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CandidateAndVoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CandidateService candidateService;

    @MockBean
    private VotingService votingService;

    @Test
    @DisplayName("GET /api/elections/{id}/candidates returns 200 with candidates")
    void getCandidates_Returns200() throws Exception {
        CandidateResponse c = new CandidateResponse(1L, 10L, "John Doe", "Manifesto", "CSE", "President", null);
        when(candidateService.getCandidatesForElection(10L)).thenReturn(List.of(c));

        mockMvc.perform(get("/api/elections/10/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("John Doe"));
    }

    @Test
    @DisplayName("POST /api/elections/{id}/candidates returns 201 for valid candidate")
    void addCandidate_Returns201() throws Exception {
        CandidateCreateRequest req = new CandidateCreateRequest("Alice", "Better labs", "ECE", "VP", null);
        CandidateResponse res = new CandidateResponse(2L, 10L, "Alice", "Better labs", "ECE", "VP", null);

        when(candidateService.addCandidate(eq(10L), any())).thenReturn(res);

        mockMvc.perform(post("/api/elections/10/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    @DisplayName("DELETE /api/candidates/{id} returns 204 No Content")
    void deleteCandidate_Returns204() throws Exception {
        doNothing().when(candidateService).deleteCandidate(2L);

        mockMvc.perform(delete("/api/candidates/2"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/elections/{id}/votes returns 201 when vote succeeds")
    void castVote_Returns201() throws Exception {
        Principal principal = () -> "VOTER01";
        VoteRequest req = new VoteRequest(501L);
        VoteResponse res = new VoteResponse("Your vote has been cast and securely recorded.", 10L, LocalDateTime.now());

        when(votingService.castVote(eq(10L), any(), eq("VOTER01"))).thenReturn(res);

        mockMvc.perform(post("/api/elections/10/votes")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Your vote has been cast and securely recorded."));
    }

    @Test
    @DisplayName("POST /api/elections/{id}/votes returns 409 Conflict when already voted")
    void castVote_AlreadyVoted_Returns409() throws Exception {
        Principal principal = () -> "VOTER01";
        VoteRequest req = new VoteRequest(501L);

        when(votingService.castVote(eq(10L), any(), eq("VOTER01")))
                .thenThrow(new AlreadyVotedException(1L, 10L));

        mockMvc.perform(post("/api/elections/10/votes")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
