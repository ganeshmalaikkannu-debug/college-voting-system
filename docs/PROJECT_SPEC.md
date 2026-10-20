# College Voting System: Final Build Specification

**Prepared for:** Ganesh  
**Build tool:** Google Antigravity  
**Project type:** Spring Boot academic project (15 marks + 1 bonus)

---

## How to use this document
1. Install JDK 17 or 21 and Git.
2. Create an empty folder named `college-voting-system`, run `git init`, and open it in Antigravity.
3. Export this document as Markdown and save it in the project as `docs/PROJECT_SPEC.md`.
4. Create the rules file from section 1 at `.agents/rules/project-rules.md`. Older Antigravity versions use `.agent/rules/`. Do not add this folder to `.gitignore`.
5. Set the agent to Planning mode. Keep terminal commands on approval.
6. Paste the starter prompt from section 2 into the agent panel.
7. After every phase: run the app yourself, make a Git commit, then tell the agent to continue.

---

## 1. Rules file (always on)
Save this as `.agents/rules/project-rules.md`.

```markdown
# Project rules: College Voting System
- Stack: Java (installed JDK 17 or 21), latest stable Spring Boot 3.x, Maven wrapper, Spring Data JPA, file-based H2, Spring Security + JWT, Jakarta Validation, springdoc-openapi, JUnit 5 + MockMvc.
- Root package: com.voting.system. Packages: controller, service, repository, model, dto, exception, config, security.
- Controllers contain no business logic. Services hold logic. Repositories hold DB access.
- Never trust client input for role, user id, vote counts or voting status.
- Never hardcode secrets. Read them from environment variables.
- Never claim a build or test passed unless you ran it and saw the result.
- Add simple-English comments to every file. The student must explain this code in a viva.
- Ask for approval before deleting files or running destructive commands.
- The full specification is in docs/PROJECT_SPEC.md. Follow it.
```

---

## 2. Starter prompt
> "Read docs/PROJECT_SPEC.md completely. You are my senior Java backend engineer, application security engineer and frontend developer. I am a second-year engineering student and I must explain and edit this code live in a viva. Build the project exactly as the specification says, phase by phase. Start in Planning mode: inspect the workspace, then produce an Implementation Plan artifact and a Task List artifact. Do not write any code until I approve the plan. After each phase, list files changed, explain design decisions in simple English, run the build and tests, fix failures, update the Task List, and wait for me to say continue. Never claim a build or test passed unless you actually ran it."

---

## 3. Working method in Antigravity
- Produce an Implementation Plan and a Task List first, and wait for approval.
- Work in the phases of section 12. Stop after each phase.
- After each phase: list files created or changed, explain important decisions, run the build and tests, and fix failures before moving on.
- Use the browser sub-agent in Phase 5 to test the full UI flow.
- At the end, produce a Walkthrough artifact with screenshots.

---

## 4. Tech stack
- **Java:** Installed JDK (17 or 21+).
- **Spring Boot:** Latest stable 3.x, Maven wrapper.
- **Dependencies:** Spring Web, Spring Data JPA with Hibernate, Spring Security with JWT (JJWT), Jakarta Bean Validation, springdoc-openapi with Swagger UI at `/swagger-ui.html`. Lombok is optional.
- **Database:** File-based H2 (`jdbc:h2:file:./data/votingdb`) so data persists after restart. Enable the H2 console only in the dev profile (`/h2-console`).
- **Profiles:** `dev` (default) and `test`.
- **Tests:** JUnit 5, Spring Boot Test, MockMvc.
- **Frontend:** HTML5, Tailwind CSS via CDN, vanilla JavaScript.
- Use dependency versions managed by Spring Boot. Do not mix incompatible versions.

---

## 5. Package structure
Root package: `com.voting.system`
- `controller`: REST endpoints only.
- `service`: business logic and transactions.
- `repository`: Spring Data JPA interfaces.
- `model`: JPA entities and enums.
- `dto`: request and response objects with validation.
- `exception`: custom exceptions and the global handler.
- `config`: application, CORS, OpenAPI and seeding configuration.
- `security`: JWT provider, JWT filter, user details service.

---

## 6. Data model

### Entities and Fields

#### 1. User
- `id` (Long, PK)
- `rollNumber` (unique, not blank)
- `name` (not blank)
- `email` (unique, valid)
- `password` (BCrypt hash)
- `role` (ADMIN or VOTER enum)
- `enabled` (boolean)

#### 2. Election
- `id` (Long, PK)
- `title` (String)
- `description` (String)
- `status` (DRAFT, UPCOMING, ACTIVE, CLOSED)
- `startTime` (LocalDateTime)
- `endTime` (LocalDateTime)
- `resultsPublished` (boolean, default false)
- `createdBy` (String / User ref)
- `createdAt` (LocalDateTime)
- `updatedAt` (LocalDateTime)
- Relationship: One-to-many with Candidate

#### 3. Candidate
- `id` (Long, PK)
- `name` (String)
- `manifesto` (String)
- `department` (String)
- `position` (String)
- `photoUrl` (optional)
- Relationship: Many-to-one with Election.
- **Note:** No `voteCount` field. Counts are calculated directly from ballots.

#### 4. VoterParticipation
- `id` (Long, PK)
- `user` (Many-to-one User)
- `election` (Many-to-one Election)
- `castTime` (LocalDateTime)
- Database unique constraint on `(user_id, election_id)`

#### 5. Ballot
- `id` (Long, PK)
- `election` (Many-to-one Election)
- `candidate` (Many-to-one Candidate)
- `castTime` (LocalDateTime)
- **Note:** No user reference, to protect ballot privacy.

### Business Rules
- `endTime` must be after `startTime`.
- Allowed status changes: `DRAFT` -> `UPCOMING` -> `ACTIVE` -> `CLOSED` only.
- Voting is allowed only when status is `ACTIVE` and current time is between `startTime` and `endTime`.
- Do NOT store a global `hasVoted` field on User.

---

## 7. REST API
All four CRUD operations must exist. Use DTOs. Never return entities directly.

| Method | Endpoint | Access | Notes |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Always assigns VOTER on the server |
| POST | `/api/auth/login` | Public | Returns JWT |
| GET | `/api/auth/me` | Authenticated | Current user profile |
| GET | `/api/elections` | Authenticated | List elections |
| GET | `/api/elections/{id}` | Authenticated | One election |
| POST | `/api/elections` | ADMIN | Create election |
| PUT | `/api/elections/{id}` | ADMIN | Update election (DRAFT only) |
| DELETE | `/api/elections/{id}` | ADMIN | Delete election (DRAFT only) |
| PATCH | `/api/elections/{id}/status` | ADMIN | Validated status transition |
| PATCH | `/api/elections/{id}/results-visibility` | ADMIN | Publish or hide results |
| GET | `/api/elections/{id}/candidates` | Authenticated | List candidates |
| POST | `/api/elections/{id}/candidates` | ADMIN | Add candidate |
| PUT | `/api/candidates/{id}` | ADMIN | Update candidate |
| DELETE | `/api/candidates/{id}` | ADMIN | Only if election is not ACTIVE or CLOSED |
| POST | `/api/elections/{id}/votes` | VOTER | Cast vote |
| GET | `/api/elections/{id}/results` | Authenticated | Hidden until published, unless ADMIN |
| GET | `/api/elections/{id}/participation` | ADMIN | Counts only. Never who voted for whom |

Status codes: `200`, `201`, `204`, `400`, `401`, `403`, `404`, `409`.

---

## 8. Voting logic
`VotingService` casts a vote in one `@Transactional` method:
1. Load the election. Return `404` if missing.
2. Check the election is `ACTIVE` and inside its time window. Otherwise throw `ElectionClosedException`.
3. Check the candidate belongs to this election. Otherwise return `400`.
4. Check whether a participation record exists. If yes, throw `AlreadyVotedException` (`409`).
5. Save `VoterParticipation`, then `Ballot`, in the same transaction. If either fails, roll back everything.
6. Catch `DataIntegrityViolationException` from the database unique constraint and return `409`. This handles concurrent double-voting attempts.
7. Calculate results by counting `Ballot` rows grouped by candidate, with percentages.

---

## 9. Security
- Hash passwords with BCrypt.
- Use JWT with signature check and expiry check.
- Read secret from `JWT_SECRET` environment variable. A clearly marked development-only default is allowed only in dev profile.
- Stateless session management.
- Role rules: only ADMIN manages elections and candidates. Only VOTER casts votes.
- Never trust client-sent role, user id, vote counts, or voting status.
- CORS configured for same-origin use. CSRF disabled because authentication uses Bearer token in Authorization header (explain in comment).
- Never log passwords, tokens, or ballot choices.
- Return clean JSON for `401` and `403`.
- Seeding: A dev-only runner creates the first ADMIN from environment variables `ADMIN_ROLL` and `ADMIN_PASSWORD`, plus demo voters. No public admin registration.
- Provide `.env.example` without real secrets.
- Stretch goal (after core works): simple login attempt limiting.

---

## 10. Validation and exceptions
- Custom exceptions: `ResourceNotFoundException`, `AlreadyVotedException`, `ElectionClosedException`, `InvalidStatusTransitionException`.
- One `@RestControllerAdvice` class named `GlobalExceptionHandler`.
- Error JSON contains: `timestamp`, `status`, `error`, `message`, `path`, and field-level errors for `MethodArgumentNotValidException`.
- Use `@NotBlank`, `@NotNull`, `@Email`, `@Size`, `@Pattern` (roll number), `@Valid`.
- Never expose stack traces to client.

---

## 11. Frontend
- Location: `src/main/resources/static/`
- Files: `index.html`, `css/styles.css`, `js/api.js`, `js/auth.js`, `js/elections.js`, `js/ballot.js`, `js/admin.js`.
- Design: Professional, responsive indigo and slate theme with accessible forms and clear typography (Tailwind CSS CDN + custom styles). Handle loading states, empty states, validation errors, network failures, expired sessions.
- Screens:
  1. Login and voter registration
  2. Dashboard listing elections with status badges
  3. Candidate cards with manifestos
  4. Ballot with confirmation dialog & button disabled after first click
  5. Neutral vote-recorded screen
  6. Results with counts and percentage bars (shown only when published)
  7. Admin dashboard: CRUD elections & candidates, status changes, publish results, view participation counts.
- Safety: `textContent` or safe DOM building. Never inject untrusted HTML.

---

## 12. Build phases
Stop after each phase and wait for approval:
- **Phase 1:** `pom.xml`, properties, profiles, entities, repositories, DB constraints (`./mvnw clean compile`)
- **Phase 2:** Exceptions, DTOs, services, voting integrity (`./mvnw clean test`)
- **Phase 3:** Controllers, validation, all endpoints (MockMvc tests & sample curl calls)
- **Phase 4:** Spring Security, JWT, roles, dev seeder (Access rule tests)
- **Phase 5:** Frontend (Full browser flow: register, login, vote, results, admin)
- **Phase 6:** Integration tests, Swagger annotations, Postman collection (`./mvnw clean test`)
- **Phase 7:** Documentation and cleanup (Final `./mvnw clean test`, real result reported)

---

## 13. Tests (JUnit 5 and MockMvc)
- Registration, duplicate roll number, duplicate email.
- Login success and wrong password.
- Role-based access: VOTER cannot create elections; anonymous gets 401.
- Election time and status validation, invalid status transitions.
- Candidate must belong to the election.
- Voting before, during, and after election.
- Duplicate vote returns 409.
- Two simultaneous votes record only one.
- Rollback when ballot insert fails.
- Results hidden until published.
- Validation error format.
- Both DB constraints and application checks.

---

## 14. Documentation deliverables (2 marks)
- `README.md`
- `docs/API.md`
- `docs/DESIGN.md` (with Mermaid diagrams)
- `postman/voting-system.postman_collection.json`
- Javadoc / simple-English comments
- `docs/VIVA_NOTES.md` (request flow, dependency injection, annotations, double-voting prevention, JWT, 15 viva Q&As, 5 practice live-edit tasks)

---

## 15. Final deliverables
- Complete compilable source code and `pom.xml`.
- `application.properties` for dev and test.
- Entities, repositories, services, DTOs, controllers, security configuration.
- Frontend HTML, CSS, JavaScript.
- Automated tests.
- `.env.example` and `.gitignore` (ignoring `data/`, `target/`, `.env`).
- All documentation from section 14.
- Walkthrough artifact with UI screenshots.

---

## 16. Rubric coverage
- Project setup and structure: 1 mark
- REST API and CRUD: 3 marks
- Data layer (JPA): 2 marks
- Core Spring concepts: 2 marks
- Validation and exceptions: 2 marks
- Code quality and testing: 1 mark
- Documentation: 2 marks
- Viva: 2 marks
- Bonus (JWT security): +1 mark
Total: 15 marks + 1 bonus.
