package com.voting.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for nominating a candidate into an election.
 */
public class CandidateCreateRequest {

    @NotBlank(message = "Candidate name is required")
    @Size(max = 100, message = "Candidate name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Manifesto is required")
    @Size(max = 3000, message = "Manifesto cannot exceed 3000 characters")
    private String manifesto;

    @Size(max = 100, message = "Department cannot exceed 100 characters")
    private String department;

    @Size(max = 100, message = "Position cannot exceed 100 characters")
    private String position;

    @Size(max = 500, message = "Photo URL cannot exceed 500 characters")
    private String photoUrl;

    public CandidateCreateRequest() {
    }

    public CandidateCreateRequest(String name, String manifesto, String department, String position, String photoUrl) {
        this.name = name;
        this.manifesto = manifesto;
        this.department = department;
        this.position = position;
        this.photoUrl = photoUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManifesto() {
        return manifesto;
    }

    public void setManifesto(String manifesto) {
        this.manifesto = manifesto;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
