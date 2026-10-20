package com.voting.system.dto;

/**
 * Candidate Response DTO.
 * Exposes candidate information without exposing entity relations directly.
 */
public class CandidateResponse {

    private Long id;
    private Long electionId;
    private String name;
    private String manifesto;
    private String department;
    private String position;
    private String photoUrl;

    public CandidateResponse() {
    }

    public CandidateResponse(Long id, Long electionId, String name, String manifesto,
                             String department, String position, String photoUrl) {
        this.id = id;
        this.electionId = electionId;
        this.name = name;
        this.manifesto = manifesto;
        this.department = department;
        this.position = position;
        this.photoUrl = photoUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getElectionId() {
        return electionId;
    }

    public void setElectionId(Long electionId) {
        this.electionId = electionId;
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
