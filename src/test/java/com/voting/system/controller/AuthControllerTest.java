package com.voting.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voting.system.dto.LoginRequest;
import com.voting.system.dto.RegisterRequest;
import com.voting.system.dto.UserProfileResponse;
import com.voting.system.exception.BadRequestException;
import com.voting.system.exception.GlobalExceptionHandler;
import com.voting.system.model.Role;
import com.voting.system.model.User;
import com.voting.system.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.voting.system.security.JwtTokenProvider;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc tests for AuthController.
 */
@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("POST /api/auth/register returns 201 for valid voter registration")
    void register_Valid_Returns201() throws Exception {
        RegisterRequest request = new RegisterRequest("ROLL001", "Bob Smith", "bob@college.edu", "secret123");
        User user = new User("ROLL001", "Bob Smith", "bob@college.edu", "hashpass", Role.VOTER);
        user.setId(10L);

        when(userService.registerUser(any(RegisterRequest.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.rollNumber").value("ROLL001"))
                .andExpect(jsonPath("$.role").value("VOTER"));
    }

    @Test
    @DisplayName("POST /api/auth/register returns 400 with field errors for invalid input")
    void register_InvalidFields_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("", "", "not-an-email", "12");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /api/auth/register returns 400 when roll number is duplicate")
    void register_DuplicateRoll_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("ROLL001", "Bob", "bob@college.edu", "secret123");
        when(userService.registerUser(any(RegisterRequest.class)))
                .thenThrow(new BadRequestException("Roll number 'ROLL001' is already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Roll number 'ROLL001' is already registered"));
    }

    @Test
    @DisplayName("POST /api/auth/login returns 200 with JWT for valid credentials")
    void login_Valid_Returns200() throws Exception {
        LoginRequest request = new LoginRequest("ROLL001", "secret123");
        User user = new User("ROLL001", "Bob", "bob@college.edu", "encoded_hash", Role.VOTER);
        user.setId(10L);

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userService.findByRollNumber("ROLL001")).thenReturn(user);
        when(jwtTokenProvider.generateToken("ROLL001", "VOTER", "Bob")).thenReturn("signed_jwt_token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed_jwt_token"))
                .andExpect(jsonPath("$.rollNumber").value("ROLL001"));
    }

    @Test
    @DisplayName("POST /api/auth/login returns 401 for wrong password")
    void login_WrongPassword_Returns401() throws Exception {
        LoginRequest request = new LoginRequest("ROLL001", "wrong_pass");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Invalid roll number or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid roll number or password"));
    }

    @Test
    @DisplayName("GET /api/auth/me returns 200 with profile for authenticated user")
    void getMe_Authenticated_Returns200() throws Exception {
        Principal mockPrincipal = () -> "ROLL001";
        UserProfileResponse profile = new UserProfileResponse(10L, "ROLL001", "Bob", "bob@college.edu", Role.VOTER, true, LocalDateTime.now());

        when(userService.getUserProfile("ROLL001")).thenReturn(profile);

        mockMvc.perform(get("/api/auth/me").principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rollNumber").value("ROLL001"));
    }
}
