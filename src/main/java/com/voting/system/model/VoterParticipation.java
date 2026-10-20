package com.voting.system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * VoterParticipation Entity tracks THAT a user voted in a specific election.
 * 
 * Simple-English Explanation:
 * Maps to the "voter_participations" table.
 * Crucial Security & Privacy Design:
 * 1. This record remembers ONLY that Student X participated in Election Y at time T.
 * 2. It DOES NOT store WHO they voted for!
 * 3. A database UNIQUE CONSTRAINT on (user_id, election_id) guarantees at the database engine level
 *    that a student can never have two rows for the same election.
 * 4. Even if two concurrent requests hit the server at the exact same millisecond, the database
 *    will reject the second insert with a unique constraint violation (DataIntegrityViolationException).
 */
@Entity
@Table(name = "voter_participations", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_election_participation", columnNames = {"user_id", "election_id"})
})
public class VoterParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @Column(nullable = false)
    private LocalDateTime castTime;

    public VoterParticipation() {
    }

    public VoterParticipation(User user, Election election, LocalDateTime castTime) {
        this.user = user;
        this.election = election;
        this.castTime = castTime;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
    }

    public LocalDateTime getCastTime() {
        return castTime;
    }

    public void setCastTime(LocalDateTime castTime) {
        this.castTime = castTime;
    }
}
