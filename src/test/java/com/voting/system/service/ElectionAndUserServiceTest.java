package com.voting.system.service;

import com.voting.system.dto.ElectionCreateRequest;
import com.voting.system.dto.ElectionResponse;
import com.voting.system.dto.RegisterRequest;
import com.voting.system.exception.BadRequestException;
import com.voting.system.exception.InvalidStatusTransitionException;
import com.voting.system.model.Election;
import com.voting.system.model.ElectionStatus;
import com.voting.system.model.Role;
import com.voting.system.model.User;
import com.voting.system.repository.CandidateRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService and ElectionService rules.
 */
@ExtendWith(MockitoExtension.class)
class ElectionAndUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    @Mock
    private ElectionRepository electionRepository;
    @Mock
    private CandidateRepository candidateRepository;
    @Mock
    private VoterParticipationRepository voterParticipationRepository;
    @InjectMocks
    private ElectionService electionService;

    @Test
    @DisplayName("User registration enforces VOTER role and hashes password")
    void registerUser_EnforcesVoterRole() {
        RegisterRequest request = new RegisterRequest("ROLL001", "Bob Smith", "bob@college.edu", "secret123");
        when(userRepository.existsByRollNumber("ROLL001")).thenReturn(false);
        when(userRepository.existsByEmail("bob@college.edu")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded_secret");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = userService.registerUser(request);

        assertThat(user.getRole()).isEqualTo(Role.VOTER);
        assertThat(user.getPassword()).isEqualTo("encoded_secret");
        assertThat(user.getRollNumber()).isEqualTo("ROLL001");
    }

    @Test
    @DisplayName("User registration rejects duplicate roll number")
    void registerUser_DuplicateRollNumber_ThrowsBadRequest() {
        RegisterRequest request = new RegisterRequest("ROLL001", "Bob", "bob@college.edu", "secret123");
        when(userRepository.existsByRollNumber("ROLL001")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("Election creation fails if endTime is before or equal to startTime")
    void createElection_InvalidTimes_ThrowsBadRequest() {
        LocalDateTime now = LocalDateTime.now();
        ElectionCreateRequest request = new ElectionCreateRequest("Test Election", "Desc", now.plusHours(2), now.plusHours(1));

        assertThatThrownBy(() -> electionService.createElection(request, "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("end time must be after start time");
    }

    @Test
    @DisplayName("Valid forward status transition succeeds (DRAFT -> UPCOMING)")
    void updateStatus_ValidTransition_Success() {
        Election election = new Election("Council", "Desc", LocalDateTime.now(), LocalDateTime.now().plusDays(1), "admin");
        election.setId(1L);
        election.setStatus(ElectionStatus.DRAFT);

        when(electionRepository.findById(1L)).thenReturn(Optional.of(election));
        when(electionRepository.save(any(Election.class))).thenAnswer(inv -> inv.getArgument(0));

        ElectionResponse response = electionService.updateStatus(1L, ElectionStatus.UPCOMING);
        assertThat(response.getStatus()).isEqualTo(ElectionStatus.UPCOMING);
    }

    @Test
    @DisplayName("Invalid backward status transition fails (CLOSED -> ACTIVE)")
    void updateStatus_InvalidTransition_ThrowsException() {
        Election election = new Election("Council", "Desc", LocalDateTime.now(), LocalDateTime.now().plusDays(1), "admin");
        election.setId(1L);
        election.setStatus(ElectionStatus.CLOSED);

        when(electionRepository.findById(1L)).thenReturn(Optional.of(election));

        assertThatThrownBy(() -> electionService.updateStatus(1L, ElectionStatus.ACTIVE))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Invalid status transition from 'CLOSED' to 'ACTIVE'");
    }
}
