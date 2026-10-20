package com.voting.system.controller;

import com.voting.system.dto.AuthResponse;
import com.voting.system.dto.LoginRequest;
import com.voting.system.dto.RegisterRequest;
import com.voting.system.dto.UserProfileResponse;
import com.voting.system.model.User;
import com.voting.system.security.JwtTokenProvider;
import com.voting.system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * REST Controller for authentication and voter registration.
 * 
 * Simple-English Explanation:
 * Handles incoming HTTP requests for:
 * - Registering new student voters (forces Role.VOTER)
 * - Authenticating users via AuthenticationManager and issuing signed JWTs
 * - Fetching the authenticated profile via /api/auth/me
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtTokenProvider tokenProvider) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        User registeredUser = userService.registerUser(request);
        UserProfileResponse response = new UserProfileResponse(
                registeredUser.getId(),
                registeredUser.getRollNumber(),
                registeredUser.getName(),
                registeredUser.getEmail(),
                registeredUser.getRole(),
                registeredUser.isEnabled(),
                registeredUser.getCreatedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String cleanRoll = request.getRollNumber().trim().toUpperCase();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(cleanRoll, request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (AuthenticationException ex) {
            throw new BadCredentialsException("Invalid roll number or password");
        }

        User user = userService.findByRollNumber(cleanRoll);
        String jwt = tokenProvider.generateToken(user.getRollNumber(), user.getRole().name(), user.getName());

        AuthResponse response = new AuthResponse(
                jwt,
                user.getId(),
                user.getRollNumber(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new BadCredentialsException("User is not authenticated");
        }
        UserProfileResponse profile = userService.getUserProfile(principal.getName());
        return ResponseEntity.ok(profile);
    }
}
