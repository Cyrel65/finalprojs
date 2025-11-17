package org.example.finalprojs.service;

import org.example.finalprojs.model.GradeReport;
import org.example.finalprojs.model.User;
import org.example.finalprojs.repository.GradeReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GradeService {

    private final GradeReportRepository gradeReportRepository;

    @Autowired
    public GradeService(GradeReportRepository gradeReportRepository) {
        this.gradeReportRepository = gradeReportRepository;
    }

    /**
     * Fetches a GradeReport, or creates an empty one if not found.
     * Always calculates the final overall grade before returning.
     */
    public GradeReport getReportForDisplay(User user, String subjectName) {
        // 1. Fetch the single GradeReport for the user and subject
        Optional<GradeReport> reportOptional = gradeReportRepository.findByUserAndSubject(user, subjectName);

        GradeReport report;

        if (reportOptional.isEmpty()) {
            // If no report found, create a new object with default values (zeros)
            report = new GradeReport();
            report.setSubject(subjectName);
            report.setUser(user);
            report.setOverallGrade(0.0);
        } else {
            report = reportOptional.get();
        }

        // 2. Calculate the overall grade based on the raw points
        double calculatedOverall = calculateOverallGrade(report);
        report.setOverallGrade(calculatedOverall);

        return report;
    }

    // --- Grade Calculation Logic (Moved from Controller) ---

    private double calculateOverallGrade(GradeReport report) {

        // --- CONSTANTS FOR WEIGHTING AND MAX POINTS ---
        final int MAX_SC_POINTS = 10;
        final int MAX_TS_POINTS = 20;
        final int MAX_UT_POINTS = 50;
        final int MAX_TT_POINTS = 50;

        final double WEIGHT_SC_TS = 0.30;
        final double WEIGHT_UT = 0.30;
        final double WEIGHT_TT = 0.30;
        final double WEIGHT_ATTENDANCE = 0.10;


        // 1. Calculate Self Check & Task Sheet Category (30% Weight)
        int totalSelfCheckTaskEarned =
                report.getSelfCheck1() + report.getSelfCheck2() + report.getSelfCheck3() +
                        report.getSelfCheck4() + report.getSelfCheck5() +
                        report.getTaskSheet1() + report.getTaskSheet2() + report.getTaskSheet3();

        int totalSelfCheckTaskMax = (5 * MAX_SC_POINTS) + (3 * MAX_TS_POINTS); // 110 max points

        double selfCheckTaskPercentage = (double) totalSelfCheckTaskEarned / totalSelfCheckTaskMax;
        double weightedSelfCheckTask = selfCheckTaskPercentage * WEIGHT_SC_TS;


        // 2. Calculate Unit Test Category (30% Weight)
        int totalUnitTestEarned = report.getUnitTest1() + report.getUnitTest2();
        int totalUnitTestMax = 2 * MAX_UT_POINTS; // 100 max points

        double unitTestPercentage = (double) totalUnitTestEarned / totalUnitTestMax;
        double weightedUnitTest = unitTestPercentage * WEIGHT_UT;


        // 3. Calculate Term Test Category (30% Weight)
        int totalTermTestEarned = report.getTermTest();
        int totalTermTestMax = MAX_TT_POINTS; // 50 max points

        double termTestPercentage = (double) totalTermTestEarned / totalTermTestMax;
        double weightedTermTest = termTestPercentage * WEIGHT_TT;


        // 4. Calculate Attendance (10% Weight)
        double attendancePercentage = (double) report.getAttendance() / 100.0; // Attendance is stored as 0-100%
        double weightedAttendance = attendancePercentage * WEIGHT_ATTENDANCE;


        // 5. Calculate Final Overall Grade and convert to display percentage (0-100)
        double overallGrade = weightedSelfCheckTask + weightedUnitTest + weightedTermTest + weightedAttendance;

        return overallGrade * 100.0;
    }
}