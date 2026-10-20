package com.voting.system.dto;

/**
 * Participation metrics for administrators.
 * 
 * Simple-English Explanation:
 * Provides turnout aggregates (e.g., 450 out of 600 students voted = 75.0% turnout).
 * CRITICAL PRIVACY RULE: Never contains who voted or who anyone voted for.
 */
public class ParticipationStatsResponse {

    private Long electionId;
    private String electionTitle;
    private long totalRegisteredVoters;
    private long votesCast;
    private double turnoutPercentage;

    public ParticipationStatsResponse() {
    }

    public ParticipationStatsResponse(Long electionId, String electionTitle, long totalRegisteredVoters,
                                      long votesCast, double turnoutPercentage) {
        this.electionId = electionId;
        this.electionTitle = electionTitle;
        this.totalRegisteredVoters = totalRegisteredVoters;
        this.votesCast = votesCast;
        this.turnoutPercentage = turnoutPercentage;
    }

    public Long getElectionId() {
        return electionId;
    }

    public void setElectionId(Long electionId) {
        this.electionId = electionId;
    }

    public String getElectionTitle() {
        return electionTitle;
    }

    public void setElectionTitle(String electionTitle) {
        this.electionTitle = electionTitle;
    }

    public long getTotalRegisteredVoters() {
        return totalRegisteredVoters;
    }

    public void setTotalRegisteredVoters(long totalRegisteredVoters) {
        this.totalRegisteredVoters = totalRegisteredVoters;
    }

    public long getVotesCast() {
        return votesCast;
    }

    public void setVotesCast(long votesCast) {
        this.votesCast = votesCast;
    }

    public double getTurnoutPercentage() {
        return turnoutPercentage;
    }

    public void setTurnoutPercentage(double turnoutPercentage) {
        this.turnoutPercentage = turnoutPercentage;
    }
}
