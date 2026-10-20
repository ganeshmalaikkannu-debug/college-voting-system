package com.voting.system.service;

import com.voting.system.dto.RegisterRequest;
import com.voting.system.dto.UserProfileResponse;
import com.voting.system.exception.BadRequestException;
import com.voting.system.exception.ResourceNotFoundException;
import com.voting.system.model.Role;
import com.voting.system.model.User;
import com.voting.system.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user registration and profiles.
 * 
 * Simple-English Explanation:
 * Handles business rules around users:
 * 1. Checks that roll numbers and emails are unique.
 * 2. Hashes raw passwords using BCrypt before storing them.
 * 3. CRITICAL SECURITY RULE: Every registered user is strictly assigned Role.VOTER on the server.
 *    There is NO public API to register as ADMIN.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(RegisterRequest request) {
        String cleanRoll = request.getRollNumber().trim().toUpperCase();
        String cleanEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByRollNumber(cleanRoll)) {
            throw new BadRequestException("Roll number '" + cleanRoll + "' is already registered");
        }

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new BadRequestException("Email '" + cleanEmail + "' is already registered");
        }

        User user = new User();
        user.setRollNumber(cleanRoll);
        user.setName(request.getName().trim());
        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // Always assign VOTER on the server regardless of client input
        user.setRole(Role.VOTER);
        user.setEnabled(true);

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findByRollNumber(String rollNumber) {
        return userRepository.findByRollNumber(rollNumber.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "rollNumber", rollNumber));
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String rollNumber) {
        User user = findByRollNumber(rollNumber);
        return new UserProfileResponse(
                user.getId(),
                user.getRollNumber(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
