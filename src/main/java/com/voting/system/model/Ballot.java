package com.voting.system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Ballot Entity represents an anonymous vote cast for a candidate in an election.
 * 
 * Simple-English Explanation:
 * Maps to the "ballots" table.
 * CRITICAL VIVA POINT:
 * Notice there is NO user reference, user_id, or voter foreign key in this table!
 * By decoupling the voter's identity (stored in VoterParticipation) from the candidate choice
 * (stored here in Ballot), we provide true ballot secrecy. Even database administrators cannot
 * directly query "who did user X vote for?".
 */
@Entity
@Table(name = "ballots")
public class Ballot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(nullable = false)
    private LocalDateTime castTime;

    public Ballot() {
    }

    public Ballot(Election election, Candidate candidate, LocalDateTime castTime) {
        this.election = election;
        this.candidate = candidate;
        this.castTime = castTime;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public LocalDateTime getCastTime() {
        return castTime;
    }

    public void setCastTime(LocalDateTime castTime) {
        this.castTime = castTime;
    }
}
