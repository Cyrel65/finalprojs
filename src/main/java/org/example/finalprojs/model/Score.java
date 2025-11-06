package org.example.finalprojs.model;

import jakarta.persistence.*;

@Entity
@Table(name = "scores")
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many Scores belong to One User
    // Assumes your User model is in the same package and named 'User'
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String subject;       // e.g., "Math"
    private String testName;      // e.g., "Quiz 1"
    private int scoreValue;       // e.g., 95 (for the score/percentage)
    private String rawGrade;      // e.g., "A+" or "75%"

    // Default Constructor (required by JPA)
    public Score() {
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    public int getScoreValue() { return scoreValue; }
    public void setScoreValue(int scoreValue) { this.scoreValue = scoreValue; }
    public String getRawGrade() { return rawGrade; }
    public void setRawGrade(String rawGrade) { this.rawGrade = rawGrade; }
}