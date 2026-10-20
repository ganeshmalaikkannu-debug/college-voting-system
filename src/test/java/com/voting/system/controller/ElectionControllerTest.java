package com.voting.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voting.system.dto.ElectionCreateRequest;
import com.voting.system.dto.ElectionResponse;
import com.voting.system.dto.ElectionResultResponse;
import com.voting.system.dto.ElectionStatusUpdateRequest;
import com.voting.system.dto.ElectionUpdateRequest;
import com.voting.system.dto.ParticipationStatsResponse;
import com.voting.system.exception.GlobalExceptionHandler;
import com.voting.system.exception.ResourceNotFoundException;
import com.voting.system.model.ElectionStatus;
import com.voting.system.service.ElectionService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for ElectionController.
 */
@WebMvcTest(ElectionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ElectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ElectionService electionService;

    @MockBean
    private VotingService votingService;

    @Test
    @DisplayName("GET /api/elections returns 200 with election list")
    void getAllElections_Returns200() throws Exception {
        ElectionResponse r = new ElectionResponse(1L, "Title", "Desc", ElectionStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), false, "admin",
                LocalDateTime.now(), LocalDateTime.now(), 2, false);

        when(electionService.getAllElections(any())).thenReturn(List.of(r));

        mockMvc.perform(get("/api/elections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Title"));
    }

    @Test
    @DisplayName("GET /api/elections/{id} returns 200 for existing election")
    void getElectionById_Found_Returns200() throws Exception {
        ElectionResponse r = new ElectionResponse(1L, "Title", "Desc", ElectionStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), false, "admin",
                LocalDateTime.now(), LocalDateTime.now(), 2, false);

        when(electionService.getElectionById(eq(1L), any())).thenReturn(r);

        mockMvc.perform(get("/api/elections/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/elections/{id} returns 404 when election not found")
    void getElectionById_NotFound_Returns404() throws Exception {
        when(electionService.getElectionById(eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("Election", "id", 999L));

        mockMvc.perform(get("/api/elections/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Election not found with id: '999'"));
    }

    @Test
    @DisplayName("POST /api/elections returns 201 for valid election creation")
    void createElection_Valid_Returns201() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        ElectionCreateRequest request = new ElectionCreateRequest("New Election", "Description", now.plusHours(1), now.plusHours(5));
        ElectionResponse response = new ElectionResponse(2L, "New Election", "Description", ElectionStatus.DRAFT,
                now.plusHours(1), now.plusHours(5), false, "ADMIN", now, now, 0, null);

        when(electionService.createElection(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/elections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("New Election"));
    }

    @Test
    @DisplayName("PUT /api/elections/{id} returns 200 for update")
    void updateElection_Returns200() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        ElectionUpdateRequest request = new ElectionUpdateRequest("Updated Title", "Updated Desc", now.plusHours(1), now.plusHours(5));
        ElectionResponse response = new ElectionResponse(1L, "Updated Title", "Updated Desc", ElectionStatus.DRAFT,
                now.plusHours(1), now.plusHours(5), false, "ADMIN", now, now, 0, null);

        when(electionService.updateElection(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/elections/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    @DisplayName("DELETE /api/elections/{id} returns 204 No Content")
    void deleteElection_Returns204() throws Exception {
        doNothing().when(electionService).deleteElection(1L);

        mockMvc.perform(delete("/api/elections/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /api/elections/{id}/status returns 200 on status transition")
    void patchStatus_Returns200() throws Exception {
        ElectionStatusUpdateRequest request = new ElectionStatusUpdateRequest(ElectionStatus.UPCOMING);
        ElectionResponse response = new ElectionResponse(1L, "Title", "Desc", ElectionStatus.UPCOMING,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), false, "admin",
                LocalDateTime.now(), LocalDateTime.now(), 2, false);

        when(electionService.updateStatus(eq(1L), eq(ElectionStatus.UPCOMING))).thenReturn(response);

        mockMvc.perform(patch("/api/elections/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPCOMING"));
    }

    @Test
    @DisplayName("GET /api/elections/{id}/participation returns 200 with counts")
    void getParticipation_Returns200() throws Exception {
        ParticipationStatsResponse stats = new ParticipationStatsResponse(1L, "Title", 100L, 75L, 75.0);
        when(votingService.getParticipationStats(1L)).thenReturn(stats);

        mockMvc.perform(get("/api/elections/1/participation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnoutPercentage").value(75.0));
    }

    @Test
    @DisplayName("GET /api/elections/{id}/results returns 200 with result data")
    void getResults_Returns200() throws Exception {
        ElectionResultResponse results = new ElectionResultResponse(1L, "Title", 50L, true, List.of());
        when(votingService.getElectionResults(eq(1L), any())).thenReturn(results);

        mockMvc.perform(get("/api/elections/1/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotesCast").value(50));
    }
}
