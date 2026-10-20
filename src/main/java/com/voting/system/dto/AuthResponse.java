package com.voting.system.dto;

import com.voting.system.model.Role;

/**
 * Authentication response returned upon successful login.
 * Contains the signed JWT Bearer token and user details.
 */
public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long id;
    private String rollNumber;
    private String name;
    private String email;
    private Role role;

    public AuthResponse() {
    }

    public AuthResponse(String token, Long id, String rollNumber, String name, String email, Role role) {
        this.token = token;
        this.tokenType = "Bearer";
        this.id = id;
        this.rollNumber = rollNumber;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
