# College Voting System — Architecture & System Design

This document details the architectural decisions, database models, cryptographic security guarantees, and transaction boundaries implemented in the College Voting System.

---

## 1. High-Level Layered Architecture

The application adopts a clean, decoupled 4-tier layered architecture adhering to Spring Boot enterprise best practices:

```mermaid
graph TD
    Client["Client Tier (Vanilla JS / Tailwind / Responsive SPA)"]
    Gateway["Security Filter Chain (JWT Auth Filter, CORS, CSRF Disabled)"]
    Controller["REST Controller Tier (AuthController, ElectionController, VoteController)"]
    Service["Service Tier (@Transactional, Domain Logic, Results Embargo)"]
    Data["Persistence Tier (Spring Data JPA Repositories)"]
    DB[("Database (File-Based H2 /data/votingdb)")]

    Client -->|HTTP / JSON + Bearer JWT| Gateway
    Gateway --> Controller
    Controller --> Service
    Service --> Data
    Data --> DB
```

---

## 2. Privacy-Preserving Database Schema (ER Diagram)

A core requirement of institutional democracy is the **Secret Ballot**: it must be mathematically and architecturally impossible for any database administrator, faculty member, or hacker to correlate a specific student with their chosen candidate.

```mermaid
erDiagram
    USERS ||--o{ VOTER_PARTICIPATION : "has participated"
    ELECTIONS ||--o{ VOTER_PARTICIPATION : "records turnout"
    ELECTIONS ||--o{ CANDIDATES : "nominates"
    ELECTIONS ||--o{ BALLOTS : "accumulates"
    CANDIDATES ||--o{ BALLOTS : "receives"

    USERS {
        bigint id PK
        varchar roll_number UK "Unique student ID"
        varchar email UK
        varchar full_name
        varchar password_hash "BCrypt $2a$10"
        varchar role "ADMIN | VOTER"
        timestamp created_at
    }

    ELECTIONS {
        bigint id PK
        varchar title
        text description
        timestamp start_time
        timestamp end_time
        varchar status "DRAFT | UPCOMING | ACTIVE | CLOSED"
        boolean results_published "Embargo flag"
        varchar created_by
    }

    CANDIDATES {
        bigint id PK
        bigint election_id FK
        varchar name
        text manifesto
        varchar department
        varchar position
        varchar photo_url
    }

    VOTER_PARTICIPATION {
        bigint id PK
        bigint user_id FK "References USERS"
        bigint election_id FK "References ELECTIONS"
        timestamp voted_at
    }

    BALLOTS {
        bigint id PK
        bigint election_id FK "References ELECTIONS"
        bigint candidate_id FK "References CANDIDATES"
        timestamp cast_time
    }
```

### Architectural Privacy Proof:
1. **Separation of Concerns:**
   - `VOTER_PARTICIPATION` answers the audit question: *"Did Student 24CS001 participate in Election #1?"*
   - `BALLOTS` answers the tally question: *"How many total votes were cast for Candidate Priya Patel in Election #1?"*
2. **Zero Foreign Key:**
   - The `BALLOTS` table contains strictly **NO `user_id` foreign key**, no IP address, no user agent, and no student roll number.
3. **No `voteCount` in `CANDIDATES` Table:**
   - Candidates do not store mutable counters (`voteCount`). Instead, certified totals are computed by aggregating individual anonymous records in `BALLOTS`. This eliminates lost updates under concurrency.

---

## 3. Atomic Voting Transaction (Sequence Diagram)

When a student clicks "Confirm & Cast My Ballot", the backend executes an atomic database transaction wrapped with `@Transactional(rollbackFor = Exception.class)`:

```mermaid
sequenceDiagram
    autonumber
    actor Voter as Student Voter
    participant Filter as JwtAuthenticationFilter
    participant Controller as VoteController
    participant Service as VotingService
    participant PartRepo as VoterParticipationRepository
    participant BallotRepo as BallotRepository
    participant DB as H2 Database

    Voter->>Filter: POST /api/elections/1/votes {"candidateId": 2} + Bearer Token
    Filter->>Controller: Authenticated Principal (rollNumber="24CS001", Role=VOTER)
    Controller->>Service: castVote(electionId=1, candidateId=2, "24CS001")
    
    rect rgb(240, 245, 255)
        Note over Service, DB: Atomic @Transactional Boundary
        Service->>DB: Check Election Status == ACTIVE & Clock within Start/End
        Service->>PartRepo: existsByUserIdAndElectionId(userId, 1)
        alt Already Participated
            Service-->>Controller: throw AlreadyVotedException (409)
        else First-Time Voter
            Service->>PartRepo: save(VoterParticipation(user, election, now))
            Note over Service, DB: Unique constraint UK_USER_ELECTION_PARTICIPATION enforced
            Service->>BallotRepo: save(Ballot(election, candidate, now))
            Service->>DB: Commit Transaction (atomic write of both records)
        end
    end

    Service-->>Controller: VoteResponse (message, electionId, castTime)
    Controller-->>Voter: HTTP 201 Created (Neutral Success Screen)
```

---

## 4. Double-Voting Concurrency Mitigation

In high-concurrency scenarios (e.g. thousands of students voting in the final minutes), an attacker might fire two simultaneous requests on separate threads to cast two votes before the first request finishes:

```text
Thread A: Read participation -> Not found ---------> Insert Participation -> Insert Ballot -> OK
Thread B: Read participation -> Not found ---------> Insert Participation -> DB Constraint Violation!
```

### Two-Tier Protection Mechanism:
1. **Tier 1 (Application Level):**
   `voterParticipationRepository.existsByUserIdAndElectionId(...)` serves as a fast early-return guard.
2. **Tier 2 (Database Level - ACID Guarantee):**
   A composite unique constraint is declared on `VOTER_PARTICIPATION`:
   ```sql
   CONSTRAINT uk_user_election_participation UNIQUE (user_id, election_id)
   ```
   If two threads bypass the application check simultaneously, the underlying database enforces the unique index, rejects the second thread with `DataIntegrityViolationException`, and rolls back the entire transaction. The exception handler converts this to `HTTP 409 Conflict`.

---

## 5. Election Lifecycle State Machine

An election progresses through strict, non-reversible lifecycle phases managed by authorized administrators:

```mermaid
stateDiagram-v2
    [*] --> DRAFT : Admin creates election
    DRAFT --> UPCOMING : Admin finalizes nominations & dates
    UPCOMING --> ACTIVE : Election window opens
    ACTIVE --> CLOSED : Voting deadline expires / Admin closes

    DRAFT --> [*] : Deleted (allowed only in DRAFT)

    note right of DRAFT
        Editable: Title, Dates, Candidates
        Voting: FORBIDDEN
        Results: FORBIDDEN
    end note

    note right of UPCOMING
        Locked: No candidate deletions
        Voting: FORBIDDEN
        Results: FORBIDDEN
    end note

    note right of ACTIVE
        Voting: PERMITTED for Voters
        Tally: EMBARGOED
        Modifications: FROZEN
    end note

    note right of CLOSED
        Voting: PERMANENTLY HALTED
        Tally: Embargoed until Published
        Public Results: Visible once Published
    end note
```

---

## 6. Results Embargo Architecture

To prevent early results from influencing voting behavior or causing voter suppression, results are strictly **embargoed**:
1. During `ACTIVE` status, the `/api/elections/{id}/results` endpoint rejects voter requests with `400 Bad Request` (`ELECTION_STILL_ACTIVE`).
2. Once status is `CLOSED`, the results remain hidden from standard voters (`403 Forbidden` `RESULTS_EMBARGOED`) until an administrator explicitly reviews the turnout and triggers:
   ```http
   PATCH /api/elections/{id}/results-visibility {"published": true}
   ```
3. Once published, all voters and guests can view certified vote tallies, percentage breakdowns, and winner announcements.
