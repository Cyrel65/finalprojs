package org.example.finalprojs.model;

import jakarta.persistence.*;
import java.lang.Math;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

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

    private int selfCheck1 = 0;
    private int selfCheck2 = 0;
    private int selfCheck3 = 0;
    private int selfCheck4 = 0;
    private int selfCheck5 = 0;
    private int taskSheet1 = 0;
    private int taskSheet2 = 0;
    private int taskSheet3 = 0;
    private int unitTest1  = 0;
    private int unitTest2  = 0;
    private int termTest   = 0;
    private int attendance = 0;
    private Double overallGrade = 0.0;

    public static final int MAX_SC_POINTS  = 10;
    public static final int MAX_TS_POINTS  = 20;
    public static final int MAX_UT_POINTS  = 50;
    public static final int MAX_TT_POINTS  = 50;
    public static final int MAX_ATTENDANCE = 100;

    // Required by JPA — do NOT remove
    public GradeReport() {}

    // Required by ClassService.updateAssessmentScore() when no report exists yet:
    //   .orElse(new GradeReport(student, subject))
    public GradeReport(User user, String subject) {
        this.user    = user;
        this.subject = subject;
    }

    // --- Getters and Setters ---

    public Long getId()             { return id; }
    public void setId(Long id)      { this.id = id; }

    public User getUser()           { return user; }
    public void setUser(User user)  { this.user = user; }

    public String getSubject()              { return subject; }
    public void setSubject(String subject)  { this.subject = subject; }

    public int getSelfCheck1()                  { return selfCheck1; }
    public void setSelfCheck1(int selfCheck1)   { this.selfCheck1 = selfCheck1; }

    public int getSelfCheck2()                  { return selfCheck2; }
    public void setSelfCheck2(int selfCheck2)   { this.selfCheck2 = selfCheck2; }

    public int getSelfCheck3()                  { return selfCheck3; }
    public void setSelfCheck3(int selfCheck3)   { this.selfCheck3 = selfCheck3; }

    public int getSelfCheck4()                  { return selfCheck4; }
    public void setSelfCheck4(int selfCheck4)   { this.selfCheck4 = selfCheck4; }

    public int getSelfCheck5()                  { return selfCheck5; }
    public void setSelfCheck5(int selfCheck5)   { this.selfCheck5 = selfCheck5; }

    public int getTaskSheet1()                  { return taskSheet1; }
    public void setTaskSheet1(int taskSheet1)   { this.taskSheet1 = taskSheet1; }

    public int getTaskSheet2()                  { return taskSheet2; }
    public void setTaskSheet2(int taskSheet2)   { this.taskSheet2 = taskSheet2; }

    public int getTaskSheet3()                  { return taskSheet3; }
    public void setTaskSheet3(int taskSheet3)   { this.taskSheet3 = taskSheet3; }

    public int getUnitTest1()                   { return unitTest1; }
    public void setUnitTest1(int unitTest1)     { this.unitTest1 = unitTest1; }

    public int getUnitTest2()                   { return unitTest2; }
    public void setUnitTest2(int unitTest2)     { this.unitTest2 = unitTest2; }

    public int getTermTest()                    { return termTest; }
    public void setTermTest(int termTest)       { this.termTest = termTest; }

    public int getAttendance()                  { return attendance; }
    public void setAttendance(int attendance)   { this.attendance = attendance; }

    public Double getOverallGrade()                     { return overallGrade; }
    public void setOverallGrade(Double overallGrade)    { this.overallGrade = overallGrade; }

    // --- Business logic ---

    public int getMaxScoreForAssessment(String assessmentName) {
        if (assessmentName == null) return 0;
        if (assessmentName.startsWith("selfCheck")) return MAX_SC_POINTS;
        if (assessmentName.startsWith("taskSheet"))  return MAX_TS_POINTS;
        if (assessmentName.startsWith("unitTest"))   return MAX_UT_POINTS;
        if (assessmentName.startsWith("termTest"))   return MAX_TT_POINTS;
        if (assessmentName.equals("attendance"))     return MAX_ATTENDANCE;
        return 0;
    }

    public int getAssessmentScore(String assessmentName) {
        if (assessmentName == null) return 0;
        switch (assessmentName) {
            case "selfCheck1": return selfCheck1;
            case "selfCheck2": return selfCheck2;
            case "selfCheck3": return selfCheck3;
            case "selfCheck4": return selfCheck4;
            case "selfCheck5": return selfCheck5;
            case "taskSheet1": return taskSheet1;
            case "taskSheet2": return taskSheet2;
            case "taskSheet3": return taskSheet3;
            case "unitTest1":  return unitTest1;
            case "unitTest2":  return unitTest2;
            case "termTest":   return termTest;
            case "attendance": return attendance;
            default:           return 0;
        }
    }

    public void addPointsToAssessment(String assessmentName, int points) {
        if (points <= 0 || assessmentName == null) return;
        int maxScore    = getMaxScoreForAssessment(assessmentName);
        int currentScore = getAssessmentScore(assessmentName);
        int finalScore  = Math.min(currentScore + points, maxScore);

        switch (assessmentName) {
            case "selfCheck1": selfCheck1 = finalScore; break;
            case "selfCheck2": selfCheck2 = finalScore; break;
            case "selfCheck3": selfCheck3 = finalScore; break;
            case "selfCheck4": selfCheck4 = finalScore; break;
            case "selfCheck5": selfCheck5 = finalScore; break;
            case "taskSheet1": taskSheet1 = finalScore; break;
            case "taskSheet2": taskSheet2 = finalScore; break;
            case "taskSheet3": taskSheet3 = finalScore; break;
            case "unitTest1":  unitTest1  = finalScore; break;
            case "unitTest2":  unitTest2  = finalScore; break;
            case "termTest":   termTest   = finalScore; break;
            case "attendance": attendance = finalScore; break;
        }
    }

    public static List<String> getAssessmentFieldNames() {
        List<String> names = new ArrayList<>();
        for (Field field : GradeReport.class.getDeclaredFields()) {
            String name = field.getName();
            if ((name.startsWith("selfCheck") || name.startsWith("taskSheet") ||
                    name.startsWith("unitTest")  || name.equals("termTest") ||
                    name.equals("attendance"))
                    && (field.getType().equals(int.class) || field.getType().equals(Integer.class))) {
                names.add(name);
            }
        }
        return names;
    }
}