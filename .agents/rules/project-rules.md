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
