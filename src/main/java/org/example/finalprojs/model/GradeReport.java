package org.example.finalprojs.model;

import jakarta.persistence.*;

@Entity
@Table(name = "grade_reports")
public class GradeReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String subject;

    // --- Raw Points for 30% Self Check & Task Sheet Category ---
    private int selfCheck1 = 0; // Max 10 pts
    private int selfCheck2 = 0; // Max 10 pts
    private int selfCheck3 = 0; // Max 10 pts
    private int selfCheck4 = 0; // Max 10 pts
    private int selfCheck5 = 0; // Max 10 pts
    private int taskSheet1 = 0; // Max 20 pts
    private int taskSheet2 = 0; // Max 20 pts
    private int taskSheet3 = 0; // Max 20 pts

    // --- Raw Points for 30% Unit Test Category ---
    private int unitTest1 = 0; // Max 50 pts
    private int unitTest2 = 0; // Max 50 pts

    // --- Raw Points for 30% Term Test Category ---
    private int termTest = 0; // Max 50 pts

    // --- 10% Attendance ---
    private int attendance = 0; // Stored as percentage (0-100)

    // --- Calculated Field (Stored for convenience) ---
    private Double overallGrade = 0.0;

    // Default Constructor (required by JPA)
    public GradeReport() {
    }

    // --- Getters and Setters ---

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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    // Self Checks
    public int getSelfCheck1() { return selfCheck1; }
    public void setSelfCheck1(int selfCheck1) { this.selfCheck1 = selfCheck1; }

    public int getSelfCheck2() { return selfCheck2; }
    public void setSelfCheck2(int selfCheck2) { this.selfCheck2 = selfCheck2; }

    public int getSelfCheck3() { return selfCheck3; }
    public void setSelfCheck3(int selfCheck3) { this.selfCheck3 = selfCheck3; }

    public int getSelfCheck4() { return selfCheck4; }
    public void setSelfCheck4(int selfCheck4) { this.selfCheck4 = selfCheck4; }

    public int getSelfCheck5() { return selfCheck5; }
    public void setSelfCheck5(int selfCheck5) { this.selfCheck5 = selfCheck5; }

    // Task Sheets
    public int getTaskSheet1() { return taskSheet1; }
    public void setTaskSheet1(int taskSheet1) { this.taskSheet1 = taskSheet1; }

    public int getTaskSheet2() { return taskSheet2; }
    public void setTaskSheet2(int taskSheet2) { this.taskSheet2 = taskSheet2; }

    public int getTaskSheet3() { return taskSheet3; }
    public void setTaskSheet3(int taskSheet3) { this.taskSheet3 = taskSheet3; }

    // Unit Tests
    public int getUnitTest1() { return unitTest1; }
    public void setUnitTest1(int unitTest1) { this.unitTest1 = unitTest1; }

    public int getUnitTest2() { return unitTest2; }
    public void setUnitTest2(int unitTest2) { this.unitTest2 = unitTest2; }

    // Term Test
    public int getTermTest() { return termTest; }
    public void setTermTest(int termTest) { this.termTest = termTest; }

    // Attendance
    public int getAttendance() { return attendance; }
    public void setAttendance(int attendance) { this.attendance = attendance; }

    // Overall Grade
    public Double getOverallGrade() { return overallGrade; }
    public void setOverallGrade(Double overallGrade) { this.overallGrade = overallGrade; }
}