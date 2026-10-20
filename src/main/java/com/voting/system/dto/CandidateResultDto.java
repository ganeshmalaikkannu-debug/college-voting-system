package com.voting.system.dto;

/**
 * Result row for a candidate in an election.
 */
public class CandidateResultDto {

    private Long candidateId;
    private String candidateName;
    private String department;
    private String position;
    private String photoUrl;
    private long voteCount;
    private double percentage;

    public CandidateResultDto() {
    }

    public CandidateResultDto(Long candidateId, String candidateName, String department,
                              String position, String photoUrl, long voteCount, double percentage) {
        this.candidateId = candidateId;
        this.candidateName = candidateName;
        this.department = department;
        this.position = position;
        this.photoUrl = photoUrl;
        this.voteCount = voteCount;
        this.percentage = percentage;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
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

    public long getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(long voteCount) {
        this.voteCount = voteCount;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}
