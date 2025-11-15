package org.example.finalprojs.model;

import jakarta.persistence.*;
import java.lang.Math; // Import Math for min() function

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

    // --- CONSTANTS FOR MAX POINTS (Defined here for centralized access) ---
    public static final int MAX_SC_POINTS = 10;
    public static final int MAX_TS_POINTS = 20;
    public static final int MAX_UT_POINTS = 50;
    public static final int MAX_TT_POINTS = 50;
    public static final int MAX_ATTENDANCE = 100;


    // Default Constructor (required by JPA)
    public GradeReport() {
    }

    // --- Getters and Setters (omitted for brevity, they remain unchanged) ---

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


    /**
     * Helper method to get the maximum possible score for a given assessment type.
     */
    public int getMaxScoreForAssessment(String assessmentName) {
        if (assessmentName == null) return 0;

        if (assessmentName.startsWith("selfCheck")) return MAX_SC_POINTS;
        if (assessmentName.startsWith("taskSheet")) return MAX_TS_POINTS;
        if (assessmentName.startsWith("unitTest")) return MAX_UT_POINTS;
        if (assessmentName.startsWith("termTest")) return MAX_TT_POINTS;
        if (assessmentName.equals("attendance")) return MAX_ATTENDANCE;

        return 0;
    }

    /**
     * Helper method to get the current raw score for a given assessment field.
     */
    public int getAssessmentScore(String assessmentName) {
        if (assessmentName == null) return 0;

        switch (assessmentName) {
            case "selfCheck1": return this.selfCheck1;
            case "selfCheck2": return this.selfCheck2;
            case "selfCheck3": return this.selfCheck3;
            case "selfCheck4": return this.selfCheck4;
            case "selfCheck5": return this.selfCheck5;
            case "taskSheet1": return this.taskSheet1;
            case "taskSheet2": return this.taskSheet2;
            case "taskSheet3": return this.taskSheet3;
            case "unitTest1": return this.unitTest1;
            case "unitTest2": return this.unitTest2;
            case "termTest": return this.termTest;
            case "attendance": return this.attendance;
            default: return 0;
        }
    }

    /**
     * Applies awarded points to the specified assessment field, ensuring the score does not
     * exceed the maximum possible points for that assessment.
     * @param assessmentName The name of the target field (e.g., "unitTest1", "selfCheck3").
     * @param points The points to be added to the current score.
     */
    public void addPointsToAssessment(String assessmentName, int points) {
        if (points <= 0 || assessmentName == null) {
            return;
        }

        int maxScore = getMaxScoreForAssessment(assessmentName);

        // Get the current score and calculate the new score
        int currentScore = getAssessmentScore(assessmentName);
        int newScore = currentScore + points;

        // Apply the cap: new score is the minimum of the calculated score and the max score.
        int finalScore = Math.min(newScore, maxScore);

        // Update the corresponding field using the final, capped score
        switch (assessmentName) {
            case "selfCheck1":
                this.selfCheck1 = finalScore;
                break;
            case "selfCheck2":
                this.selfCheck2 = finalScore;
                break;
            case "selfCheck3":
                this.selfCheck3 = finalScore;
                break;
            case "selfCheck4":
                this.selfCheck4 = finalScore;
                break;
            case "selfCheck5":
                this.selfCheck5 = finalScore;
                break;
            case "taskSheet1":
                this.taskSheet1 = finalScore;
                break;
            case "taskSheet2":
                this.taskSheet2 = finalScore;
                break;
            case "taskSheet3":
                this.taskSheet3 = finalScore;
                break;
            case "unitTest1":
                this.unitTest1 = finalScore;
                break;
            case "unitTest2":
                this.unitTest2 = finalScore;
                break;
            case "termTest":
                this.termTest = finalScore;
                break;
            case "attendance":
                this.attendance = finalScore;
                break;
            default:
                System.err.println("Warning: Invalid assessment name provided for redemption: " + assessmentName);
                break;
        }
    }
}