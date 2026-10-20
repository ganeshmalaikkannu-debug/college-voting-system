package com.voting.system.security;

import com.voting.system.model.User;
import com.voting.system.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Custom UserDetailsService bridging database User records with Spring Security.
 * 
 * Simple-English Explanation:
 * When a user logs in, Spring Security calls this service to fetch the user
 * from the database by roll number and verifies their password and assigned roles.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String rollNumber) throws UsernameNotFoundException {
        User user = userRepository.findByRollNumber(rollNumber.trim().toUpperCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with roll number: " + rollNumber));

        return new org.springframework.security.core.userdetails.User(
                user.getRollNumber(),
                user.getPassword(),
                user.isEnabled(),
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
