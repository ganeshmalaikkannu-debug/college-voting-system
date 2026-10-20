# College Voting System — Viva Voce Preparation Guide

**Author:** Ganesh  
**Project:** Academic Audit-Compliant Institutional Voting System  
**Framework:** Spring Boot 3.3.4, Java 21, Spring Security 6, H2 File Database, Tailwind CSS SPA  
**Audience:** External Examiner / Project Review Panel  

---

## 1. Project Summary in 60 Seconds (Elevator Pitch)

> *"Good morning, Professors. My project is a secure, audit-compliant College Voting System designed for university elections such as Student Council and Department Representatives.*  
>  
> *The central problem in electronic voting is balancing **Auditability** (proving only eligible students voted once) with **Privacy** (guaranteeing nobody can discover who a student voted for).*  
>  
> *To solve this, I designed a **detached dual-entity database architecture**: the `VoterParticipation` table records THAT a student voted to enforce one-person-one-vote, while the `Ballot` table records the candidate choice with strictly ZERO link to the voter. Both tables are committed inside an atomic Spring `@Transactional` boundary.*  
>  
> *The system features stateless JWT authentication, Role-Based Access Control (Admin vs Voter), double-voting concurrency prevention at the database constraint level, results embargoing, and a modern responsive dashboard."*

---

## 2. 15 Key Viva Questions & Bulletproof Answers

### Q1: Why did you choose Spring Boot instead of plain Servlets or PHP?
**Answer:**
- **Inversion of Control (IoC) & Dependency Injection:** Decouples business logic from object lifecycles, making code modular and testable.
- **Declarative Transactions (`@Transactional`):** Automatically manages database commit/rollback without writing manual boilerplate SQL transaction blocks.
- **Enterprise Security:** Spring Security provides an industry-standard filter chain for JWT resolution, stateless sessions, and method-level authorization (`@PreAuthorize`).
- **Production-Ready Ecosystem:** Integrated validation (`Jakarta Bean Validation`), ORM (`Spring Data JPA`), and automated interactive documentation (`SpringDoc OpenAPI`).

---

### Q2: How did you ensure ballot secrecy (voter anonymity)?
**Answer:**
- By **decoupling participation from ballot choice** across two tables:
  1. `VoterParticipation`: Stores `(user_id, election_id, voted_at)`.
  2. `Ballots`: Stores `(election_id, candidate_id, cast_time)`.
- **Zero Foreign Key:** There is no foreign key or reference from `Ballots` to `Users`.
- Even if a system administrator inspects the raw H2 database file or runs SQL queries, they cannot correlate which candidate a student voted for.

---

### Q3: Why is there no `voteCount` column in the `Candidates` table?
**Answer:**
- **Concurrency & Lost Updates:** If multiple voters vote simultaneously and update `candidate.voteCount = candidate.voteCount + 1`, race conditions can cause lost updates without pessimistic database locking.
- **Data Integrity & Auditability:** A mutable number can be manipulated directly via SQL. By keeping individual rows in `Ballots` and computing `COUNT(ballot_id) GROUP BY candidate_id`, the tally is mathematically derived directly from authentic votes.

---

### Q4: How does your system prevent double voting if two requests arrive at the exact same millisecond?
**Answer:**
- We implement **Two-Tier Protection**:
  1. **Tier 1 (Application level):** An early check `voterParticipationRepository.existsByUserIdAndElectionId(...)` quickly rejects subsequent requests.
  2. **Tier 2 (Database level):** A database unique constraint:
     ```sql
     CONSTRAINT uk_user_election_participation UNIQUE (user_id, election_id)
     ```
  - If two concurrent threads bypass the first check simultaneously, the database unique index halts the second thread with a `DataIntegrityViolationException`.
  - Spring's transaction manager rolls back the transaction, and our `GlobalExceptionHandler` translates it into an `HTTP 409 Conflict`.

---

### Q5: What does `@Transactional` do on the `castVote` method?
**Answer:**
- It defines an **ACID transaction boundary**.
- Saving the `VoterParticipation` record and saving the `Ballot` record must be **all-or-nothing**:
  - If saving the ballot fails, the voter participation record is rolled back (the student is not falsely marked as having voted).
  - If saving voter participation fails, no orphan ballot is persisted.

---

### Q6: Why did you disable CSRF (`csrf.disable()`) in `SecurityConfig`?
**Answer:**
- **CSRF (Cross-Site Request Forgery)** exploits ambient browser cookie storage (where browsers automatically attach session cookies to third-party requests).
- Our REST application is **completely stateless** and does not use HTTP cookies or server-side sessions.
- All requests require an explicit `Authorization: Bearer <JWT>` header sent via JavaScript, which malicious third-party websites cannot forge or read due to browser Same-Origin Policy. Hence, CSRF protection is unnecessary and safe to disable.

---

### Q7: How does JWT authentication work in your application?
**Answer:**
1. Voter enters roll number and password at `/api/auth/login`.
2. `AuthenticationManager` verifies the BCrypt password hash.
3. `JwtTokenProvider` signs a token with claims (`sub`, `role`, `fullName`, `exp`) using HMAC-SHA256 and our secret key.
4. On subsequent requests, `JwtAuthenticationFilter` intercepts the request, validates the cryptographic signature, extracts the user details, and populates `SecurityContextHolder`.

---

### Q8: Why did you hash passwords with BCrypt instead of SHA-256 or MD5?
**Answer:**
- MD5 and SHA-256 are fast cryptographic hash algorithms designed for data integrity; they are vulnerable to rapid brute-force dictionary attacks (billions of hashes per second on GPUs).
- **BCrypt is an adaptive key derivation function:**
  - It automatically includes a secure random salt to thwart Rainbow Table attacks.
  - It uses a configurable work factor (computational cost), making brute-force attacks computationally infeasible.

---

### Q9: How did you prevent privilege escalation (e.g., a voter registering as ADMIN)?
**Answer:**
- The registration endpoint `/api/auth/register` accepts `RegisterRequest`, which **does not contain any `role` field**.
- The `UserService.registerVoter` method explicitly and unconditionally assigns `user.setRole(Role.VOTER)`.
- Admin accounts cannot be self-registered; they are initialized securely through server environment configurations and the `DataSeeder` runner.

---

### Q10: What is an "embargoed" election result?
**Answer:**
- During an active election, releasing interim results can create a "bandwagon effect" or cause voter suppression.
- The results endpoint `/api/elections/{id}/results` blocks voter access while the election is `ACTIVE` and remains blocked even when `CLOSED` until an administrator reviews turnout and explicitly sets `resultsPublished = true`.

---

### Q11: Why did you use file-based H2 (`./data/votingdb`) instead of an in-memory database?
**Answer:**
- An in-memory database (`jdbc:h2:mem:...`) wipes all data every time the server restarts.
- File-based H2 stores tables in `./data/votingdb.mv.db`, ensuring elections, registered voters, and cast ballots **persist across server reboots**, which is essential for auditability and realistic campus deployment.

---

### Q12: How does `GlobalExceptionHandler` improve API security and user experience?
**Answer:**
- It intercepts exceptions globally (`@RestControllerAdvice`) and formats them into a clean, uniform JSON response.
- **Security:** It prevents raw Java stack traces from leaking to the client, which could expose internal class names, SQL queries, or library versions to attackers.

---

### Q13: What is the election lifecycle state machine?
**Answer:**
- Elections transition through 4 sequential states:
  1. `DRAFT`: Nominations and configuration (editable, deletable).
  2. `UPCOMING`: Finalized nominations, locked from deletion.
  3. `ACTIVE`: Voting window is open; nominations are frozen.
  4. `CLOSED`: Voting is permanently halted; results can be audited and published.

---

### Q14: What are the privacy limitations of this architecture, and how can they be mitigated?
**Answer:**
- **Timing Correlation:** If only 1 student votes between 2:00 PM and 2:05 PM, matching the timestamps between `voter_participation` and `ballots` could reveal their vote.
- **Mitigation:**
  - Introduce random artificial time-jitter/batching when recording ballots.
  - Require a minimum quorum (e.g. at least 10 votes in a batch before publishing tallies).

---

### Q15: How would you scale this application to 100,000 students across 10 campuses?
**Answer:**
1. **Database:** Migrate from H2 to PostgreSQL or MySQL with primary-replica clustering and connection pooling (HikariCP).
2. **Caching:** Cache election metadata and candidate profiles using Redis to reduce database read load during peak voting hours.
3. **Stateless Web Nodes:** Because JWT authentication is stateless, multiple Spring Boot instances can sit behind an NGINX load balancer without sticky sessions.
4. **Message Queue:** Use Apache Kafka or RabbitMQ to queue vote processing during high-volume spikes.

---

## 3. Live-Editing Challenge Drills (Professor Coding Scenarios)

### Drill 1: Professor says: "Add a minimum GPA or Semester field to the Candidate entity."
**Steps to implement in under 3 minutes:**
1. Open `src/main/java/com/voting/system/model/Candidate.java`:
   - Add field:
     ```java
     @Column(nullable = false)
     private Double gpa = 3.0;
     ```
   - Add getter and setter.
2. Open `CandidateCreateRequest.java` and `CandidateResponse.java`:
   - Add `private Double gpa;` with `@Min(0)` and `@Max(4)`.
3. Open `CandidateService.java`:
   - Map `candidate.setGpa(request.getGpa());` in `nominateCandidate()`.

---

### Drill 2: Professor says: "Change the JWT expiration time from 24 hours to 15 minutes."
**Steps to implement in 30 seconds:**
1. Open `src/main/resources/application.properties` (or `application-dev.properties`).
2. Update:
   ```properties
   app.jwt.expiration-ms=900000
   ```
   *(Explanation: 15 minutes × 60 seconds × 1000 milliseconds = 900,000 ms).*

---

### Drill 3: Professor says: "Add an endpoint allowing an Admin to extend an active election's end time."
**Steps to implement in 2 minutes:**
1. Open `ElectionService.java`:
   ```java
   public ElectionResponse extendElection(Long electionId, LocalDateTime newEndTime) {
       Election election = electionRepository.findById(electionId)
               .orElseThrow(() -> new ResourceNotFoundException("Election", "id", electionId));
       if (newEndTime.isBefore(election.getEndTime())) {
           throw new BadRequestException("New end time must be after current end time");
       }
       election.setEndTime(newEndTime);
       return mapToResponse(electionRepository.save(election));
   }
   ```
2. Open `ElectionController.java`:
   ```java
   @PatchMapping("/{id}/extend")
   public ResponseEntity<ElectionResponse> extendElection(
           @PathVariable Long id, 
           @RequestBody Map<String, String> body) {
       LocalDateTime newEndTime = LocalDateTime.parse(body.get("newEndTime"));
       return ResponseEntity.ok(electionService.extendElection(id, newEndTime));
   }
   ```

---

### Drill 4: Professor says: "Add automatic 'NOTA' (None of the Above) candidate upon election activation."
**Steps to implement in 1 minute:**
1. Open `ElectionService.java` inside the `updateStatus` method when transitioning to `ACTIVE`:
   ```java
   if (newStatus == ElectionStatus.ACTIVE) {
       boolean hasNota = candidateRepository.findByElectionId(election.getId()).stream()
               .anyMatch(c -> "NOTA".equalsIgnoreCase(c.getName()));
       if (!hasNota) {
           Candidate nota = new Candidate("NOTA", "None of the Above", "General", "None", "", election);
           candidateRepository.save(nota);
       }
   }
   ```

---

### Drill 5: Professor says: "Show me where double-voting is prevented in the code."
**Where to navigate immediately:**
1. Show `VoterParticipation.java` line 21:
   ```java
   @Table(name = "voter_participation", uniqueConstraints = {
       @UniqueConstraint(name = "uk_user_election_participation", columnNames = {"user_id", "election_id"})
   })
   ```
2. Show `VotingService.java` lines 112-125:
   - Point out the `existsByUserIdAndElectionId` check.
   - Point out the `try { voterParticipationRepository.save(...) } catch (DataIntegrityViolationException ex)` block that translates database constraints into `AlreadyVotedException`.
3. Point out `ConcurrencyAndEmbargoIntegrationTest.java`:
   - Show the multi-threaded race condition test running 2 threads concurrently with `CountDownLatch`.
