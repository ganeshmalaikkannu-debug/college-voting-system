package com.voting.system.model;

/**
 * User roles for access control.
 * 
 * Simple-English Explanation:
 * - ADMIN: College election administrator who creates elections, adds candidates, and publishes results.
 * - VOTER: Registered student who can view active elections and cast exactly one vote per election.
 */
public enum Role {
    ADMIN,
    VOTER
}
