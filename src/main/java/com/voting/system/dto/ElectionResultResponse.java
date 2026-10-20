package com.voting.system.dto;

import java.util.List;

/**
 * Election Results DTO.
 * Embargoed until resultsPublished is true, unless the requester is an ADMIN.
 */
public class ElectionResultResponse {

    private Long electionId;
    private String electionTitle;
    private long totalVotesCast;
    private boolean resultsPublished;
    private List<CandidateResultDto> candidateResults;

    public ElectionResultResponse() {
    }

    public ElectionResultResponse(Long electionId, String electionTitle, long totalVotesCast,
                                  boolean resultsPublished, List<CandidateResultDto> candidateResults) {
        this.electionId = electionId;
        this.electionTitle = electionTitle;
        this.totalVotesCast = totalVotesCast;
        this.resultsPublished = resultsPublished;
        this.candidateResults = candidateResults;
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

    public long getTotalVotesCast() {
        return totalVotesCast;
    }

    public void setTotalVotesCast(long totalVotesCast) {
        this.totalVotesCast = totalVotesCast;
    }

    public boolean isResultsPublished() {
        return resultsPublished;
    }

    public void setResultsPublished(boolean resultsPublished) {
        this.resultsPublished = resultsPublished;
    }

    public List<CandidateResultDto> getCandidateResults() {
        return candidateResults;
    }

    public void setCandidateResults(List<CandidateResultDto> candidateResults) {
        this.candidateResults = candidateResults;
    }
}
