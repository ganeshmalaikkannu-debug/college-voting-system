package com.voting.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration request payload.
 * 
 * Simple-English Explanation:
 * Notice there is NO "role" field here.
 * Even if an attacker passes "role": "ADMIN", the server completely ignores it
 * and ALWAYS assigns the VOTER role during registration.
 */
public class RegisterRequest {

    @NotBlank(message = "Roll number is required")
    @Size(min = 3, max = 50, message = "Roll number must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Roll number must contain only alphanumeric characters, underscores, or hyphens")
    private String rollNumber;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    public RegisterRequest() {
    }

    public RegisterRequest(String rollNumber, String name, String email, String password) {
        this.rollNumber = rollNumber;
        this.name = name;
        this.email = email;
        this.password = password;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
