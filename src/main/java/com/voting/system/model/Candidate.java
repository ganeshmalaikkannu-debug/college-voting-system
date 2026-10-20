package com.voting.system.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * Candidate Entity representing a nominee running in an election.
 * 
 * Simple-English Explanation:
 * Maps to the "candidates" table.
 * Each candidate belongs to one Election.
 * CRITICAL VIVA POINT: Notice there is NO "voteCount" field stored in this table!
 * If we stored voteCount here, concurrent votes could overwrite each other (lost updates),
 * and vote counts could be tampered with. Instead, results are securely computed by counting
 * authentic rows in the Ballot table.
 */
@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String manifesto;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String position;

    @Column(length = 500)
    private String photoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false)
    @JsonIgnore
    private Election election;

    public Candidate() {
    }

    public Candidate(String name, String manifesto, String department, String position, String photoUrl, Election election) {
        this.name = name;
        this.manifesto = manifesto;
        this.department = department;
        this.position = position;
        this.photoUrl = photoUrl;
        this.election = election;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
    }
}
