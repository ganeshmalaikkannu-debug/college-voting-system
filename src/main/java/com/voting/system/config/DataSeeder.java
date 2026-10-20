package com.voting.system.config;

import com.voting.system.model.*;
import com.voting.system.repository.CandidateRepository;
import com.voting.system.repository.ElectionRepository;
import com.voting.system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Development-only Data Seeder.
 * 
 * Simple-English Explanation:
 * Runs when the application boots under the "dev" profile.
 * Automatically seeds:
 * 1. The initial College Administrator account from environment variables.
 * 2. Demo student voters.
 * 3. Sample elections (one ACTIVE for live voting, one UPCOMING) with candidates.
 * 
 * CRITICAL SECURITY RULE:
 * This class is annotated with @Profile("dev"). It NEVER runs in production or testing.
 */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.roll:ADM001}")
    private String adminRoll;

    @Value("${app.admin.password:AdminSecure@123}")
    private String adminPassword;

    public DataSeeder(UserRepository userRepository,
                      ElectionRepository electionRepository,
                      CandidateRepository candidateRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.electionRepository = electionRepository;
        this.candidateRepository = candidateRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking database seeding status for 'dev' profile...");

        // 1. Seed Administrator
        if (!userRepository.existsByRollNumber(adminRoll)) {
            User admin = new User();
            admin.setRollNumber(adminRoll);
            admin.setName("College Administrator");
            admin.setEmail("admin@college.edu");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
            log.info("Seeded initial ADMIN account: {}", adminRoll);
        }

        // 2. Seed Demo Voters
        seedVoterIfNotExists("24CS001", "Alice Johnson", "alice@college.edu", "Student@123");
        seedVoterIfNotExists("24CS002", "Bob Smith", "bob@college.edu", "Student@123");
        seedVoterIfNotExists("24CS003", "Charlie Davis", "charlie@college.edu", "Student@123");

        // 3. Seed Sample Elections and Candidates
        if (electionRepository.count() == 0) {
            LocalDateTime now = LocalDateTime.now();

            // Active Election
            Election councilElection = new Election();
            councilElection.setTitle("Student Council Election 2026");
            councilElection.setDescription("Annual general election for student leadership and college representation.");
            councilElection.setStatus(ElectionStatus.ACTIVE);
            councilElection.setStartTime(now.minusHours(4));
            councilElection.setEndTime(now.plusDays(3));
            councilElection.setResultsPublished(false);
            councilElection.setCreatedBy(adminRoll);
            Election savedCouncil = electionRepository.save(councilElection);

            // Add Candidates
            candidateRepository.save(new Candidate(
                    "Aarav Sharma",
                    "Dedicated to expanding 24/7 campus library hours, modernizing computer labs, and improving cafeteria nutrition.",
                    "Computer Science",
                    "President",
                    null,
                    savedCouncil
            ));

            candidateRepository.save(new Candidate(
                    "Priya Patel",
                    "Advocating for student welfare, enhanced mental health resources, campus WiFi upgrades, and cultural arts funding.",
                    "Electronics & Comm.",
                    "President",
                    null,
                    savedCouncil
            ));

            candidateRepository.save(new Candidate(
                    "Rohan Verma",
                    "Championing industrial internship fairs, sports infrastructure renovation, and green campus sustainability.",
                    "Mechanical Eng.",
                    "President",
                    null,
                    savedCouncil
            ));

            // Upcoming Election
            Election techClubElection = new Election();
            techClubElection.setTitle("Robotics & AI Society President Election");
            techClubElection.setDescription("Nomination and selection of the Robotics Lab project leads.");
            techClubElection.setStatus(ElectionStatus.UPCOMING);
            techClubElection.setStartTime(now.plusDays(1));
            techClubElection.setEndTime(now.plusDays(5));
            techClubElection.setResultsPublished(false);
            techClubElection.setCreatedBy(adminRoll);
            Election savedTech = electionRepository.save(techClubElection);

            candidateRepository.save(new Candidate(
                    "Vikram Singh",
                    "Autonomous rover research and funding for national robotics hackathons.",
                    "Robotics Eng.",
                    "President",
                    null,
                    savedTech
            ));

            log.info("Seeded demo elections and candidate nominations.");
        }

        log.info("Database seeding completed successfully.");
    }

    private void seedVoterIfNotExists(String roll, String name, String email, String password) {
        if (!userRepository.existsByRollNumber(roll)) {
            User voter = new User();
            voter.setRollNumber(roll);
            voter.setName(name);
            voter.setEmail(email);
            voter.setPassword(passwordEncoder.encode(password));
            voter.setRole(Role.VOTER);
            voter.setEnabled(true);
            userRepository.save(voter);
            log.info("Seeded demo voter: {}", roll);
        }
    }
}
