package org.example.finalprojs;

import org.example.finalprojs.model.Score;
import org.example.finalprojs.model.User;
import org.example.finalprojs.repository.ScoreRepository;
import org.example.finalprojs.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional;

@Component
public class TestDataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScoreRepository scoreRepository;

    @Override
    public void run(String... args) throws Exception {
        // NOTE: This will insert the data every time the app starts.
        // DELETE this component or comment out the code once testing is complete.

        System.out.println("--- Inserting Test Scores for Verification ---");

        // 1. Ensure you have two distinct test users (if not, create them)
        Optional<User> studentAOptional = userRepository.findByEmail("student.a@test.com");
        Optional<User> studentBOptional = userRepository.findByEmail("student.b@test.com");

        User studentA;
        User studentB;

        if (studentAOptional.isEmpty()) {
            studentA = new User();
            studentA.setEmail("student.a@test.com");
            studentA.setPassword("pass123");
            studentA.setName("Alice Student");
            userRepository.save(studentA);
        } else {
            studentA = studentAOptional.get();
        }

        if (studentBOptional.isEmpty()) {
            studentB = new User();
            studentB.setEmail("student.b@test.com");
            studentB.setPassword("pass123");
            studentB.setName("Bob Student");
            userRepository.save(studentB);
        } else {
            studentB = studentBOptional.get();
        }


        // 2. Insert Scores for Student A (Alice)

        // Score for CP 1
        Score scoreA1 = new Score();
        scoreA1.setUser(studentA);
        scoreA1.setSubject("CP 1");
        scoreA1.setTestName("Midterm Exam");
        scoreA1.setScoreValue(88);
        scoreA1.setRawGrade("B+");
        scoreRepository.save(scoreA1);

        // Score for IM
        Score scoreA2 = new Score();
        scoreA2.setUser(studentA);
        scoreA2.setSubject("IM");
        scoreA2.setTestName("Final Project");
        scoreA2.setScoreValue(95);
        scoreA2.setRawGrade("A");
        scoreRepository.save(scoreA2);

        // Another Score for CP 1
        Score scoreA3 = new Score();
        scoreA3.setUser(studentA);
        scoreA3.setSubject("CP 1");
        scoreA3.setTestName("Quiz 3");
        scoreA3.setScoreValue(72);
        scoreA3.setRawGrade("C-");
        scoreRepository.save(scoreA3);


        // 3. Insert Scores for Student B (Bob) - Different results

        // Score for CP 1
        Score scoreB1 = new Score();
        scoreB1.setUser(studentB);
        scoreB1.setSubject("CP 1");
        scoreB1.setTestName("Midterm Exam");
        scoreB1.setScoreValue(65);
        scoreB1.setRawGrade("D");
        scoreRepository.save(scoreB1);

        // Score for ITEL
        Score scoreB2 = new Score();
        scoreB2.setUser(studentB);
        scoreB2.setSubject("ITEL");
        scoreB2.setTestName("Final Paper");
        scoreB2.setScoreValue(99);
        scoreB2.setRawGrade("A+");
        scoreRepository.save(scoreB2);

        System.out.println("--- Test Scores Inserted Successfully ---");
    }
}