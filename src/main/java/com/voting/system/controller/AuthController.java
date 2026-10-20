package com.voting.system.controller;

import com.voting.system.dto.AuthResponse;
import com.voting.system.dto.LoginRequest;
import com.voting.system.dto.RegisterRequest;
import com.voting.system.dto.UserProfileResponse;
import com.voting.system.exception.ResourceNotFoundException;
import com.voting.system.model.User;
import com.voting.system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * REST Controller for authentication and voter registration.
 * 
 * Simple-English Explanation:
 * Handles incoming HTTP requests for:
 * - Registering new student voters
 * - Logging in with roll number and password
 * - Fetching the authenticated profile
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
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
        User user;
        try {
            user = userService.findByRollNumber(request.getRollNumber());
        } catch (ResourceNotFoundException ex) {
            throw new BadCredentialsException("Invalid roll number or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid roll number or password");
        }

        // Return authentication response
        // Note: Full cryptographic JWT signing is integrated in Phase 4 SecurityConfig
        AuthResponse response = new AuthResponse(
                "token_" + user.getRollNumber() + "_" + System.currentTimeMillis(),
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
