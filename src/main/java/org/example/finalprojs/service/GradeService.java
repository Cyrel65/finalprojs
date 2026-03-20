package org.example.finalprojs.service;

import org.example.finalprojs.model.GradeReport;
import org.example.finalprojs.model.User;
import org.example.finalprojs.repository.GradeReportRepository;
import org.example.finalprojs.repository.TeacherClassRepository;
import org.example.finalprojs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GradeService {

    private final GradeReportRepository gradeReportRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final UserRepository userRepository;

    @Autowired
    public GradeService(GradeReportRepository gradeReportRepository,
                        TeacherClassRepository teacherClassRepository,
                        UserRepository userRepository) {
        this.gradeReportRepository  = gradeReportRepository;
        this.teacherClassRepository = teacherClassRepository;
        this.userRepository         = userRepository;
    }

    // ── GRADE FORMULA ─────────────────────────────────────────────────────────
    //
    //  SC + TS (30%) = (sc1+sc2+sc3+sc4+sc5+ts1+ts2+ts3) ÷ 110 × 30
    //                   max: 5×10 + 3×20 = 50+60 = 110
    //
    //  Unit Tests (30%) = (ut1+ut2) ÷ 100 × 30
    //                      max: 2×50 = 100
    //
    //  Term Test (30%)  = termTest ÷ 50 × 30
    //                      max: 50
    //
    //  Attendance (10%) = attendance ÷ 100 × 10
    //                      max: 100
    //
    //  Overall = SC+TS + UT + TT + Attendance  (max = 100.0)

    public static double calculateOverallGrade(GradeReport r) {
        double scTs = (r.getSelfCheck1() + r.getSelfCheck2() + r.getSelfCheck3()
                + r.getSelfCheck4() + r.getSelfCheck5()
                + r.getTaskSheet1() + r.getTaskSheet2() + r.getTaskSheet3())
                / 110.0 * 30.0;

        double ut   = (r.getUnitTest1() + r.getUnitTest2()) / 100.0 * 30.0;

        double tt   = r.getTermTest() / 50.0 * 30.0;

        double att  = r.getAttendance() / 100.0 * 10.0;

        double overall = scTs + ut + tt + att;

        // Round to 2 decimal places
        return Math.round(overall * 100.0) / 100.0;
    }

    // ── GRADE EQUIVALENCE TABLE ──────────────────────────────────────────────
    // For undergraduate students:
    // 99-100 = 1.0 | 96-98 = 1.25 | 93-95 = 1.50 | 90-92 = 1.75
    // 87-89  = 2.0 | 84-86 = 2.25 | 81-83 = 2.50 | 78-80 = 2.75 (implied)
    // 75-77  = 3.0 | 74 and below  = 5.0 (Dropped/Failed)
    public static double getGradeEquivalence(double percentage) {
        if (percentage >= 99) return 1.00;
        if (percentage >= 96) return 1.25;
        if (percentage >= 93) return 1.50;
        if (percentage >= 90) return 1.75;
        if (percentage >= 87) return 2.00;
        if (percentage >= 84) return 2.25;
        if (percentage >= 81) return 2.50;
        if (percentage >= 78) return 2.75;
        if (percentage >= 75) return 3.00;
        return 5.00; // Dropped
    }

    // ── RANKINGS ──────────────────────────────────────────────────────────────

    /**
     * GET /api/grades/rankings/{section}
     * Returns ranked students for a SECTION.
     * Flutter StudentRankingsScreen calls this — it passes section name.
     * Finds all subjects taught in that section, then ranks all students
     * in that section by their average overall grade across all subjects.
     */
    public List<Map<String, Object>> getRankingsBySection(String section) {
        // Get all students in this section
        List<User> students = userRepository.findBySection(section);
        if (students.isEmpty()) return Collections.emptyList();

        // Get all subjects taught in this section
        List<String> subjects = teacherClassRepository.findBySection(section)
                .stream()
                .map(tc -> tc.getSubject())
                .distinct()
                .toList();

        if (subjects.isEmpty()) {
            // Fallback: rank by any grade report the student has
            return rankStudentsByAverageGrade(students, null);
        }

        return rankStudentsByAverageGrade(students, subjects);
    }

    /**
     * GET /api/grades/rankings/subject/{subject}/section/{section}
     * Returns ranked students for a specific SUBJECT + SECTION.
     * Used when viewing rankings per class.
     */
    public List<Map<String, Object>> getRankingsBySubjectAndSection(
            String subject, String section) {

        List<User> students = userRepository.findBySection(section);
        if (students.isEmpty()) return Collections.emptyList();

        List<Map<String, Object>> result = new ArrayList<>();

        for (User student : students) {
            Optional<GradeReport> reportOpt =
                    gradeReportRepository.findByUserAndSubject(student, subject);

            double overall = reportOpt.map(GradeService::calculateOverallGrade)
                    .orElse(0.0);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("studentId",    student.getId());
            entry.put("name",         student.getName());
            entry.put("email",        student.getEmail());
            entry.put("section",      student.getSection());
            entry.put("subject",      subject);
            double equivalence = getGradeEquivalence(overall);
            entry.put("overallGrade", overall);
            entry.put("equivalence",  equivalence);
            entry.put("score",        overall); // Flutter uses 'score' key
            result.add(entry);
        }

        // Sort: by equivalence ASC (1.0 is best), then raw grade DESC as tiebreaker
        result.sort((a, b) -> {
            double eqA = (Double) a.get("equivalence");
            double eqB = (Double) b.get("equivalence");
            if (eqA != eqB) return Double.compare(eqA, eqB); // lower equivalence = better
            return Double.compare((Double) b.get("overallGrade"), (Double) a.get("overallGrade"));
        });

        // Add rank — students with same equivalence share same rank
        int rank = 1;
        for (int i = 0; i < result.size(); i++) {
            if (i > 0) {
                double prevEq = (Double) result.get(i - 1).get("equivalence");
                double currEq = (Double) result.get(i).get("equivalence");
                if (currEq != prevEq) rank = i + 1;
            }
            result.get(i).put("rank", rank);
        }

        return result;
    }

    /**
     * Helper: rank a list of students by their average overall grade
     * across the given subjects. If subjects is null, uses all their reports.
     */
    private List<Map<String, Object>> rankStudentsByAverageGrade(
            List<User> students, List<String> subjects) {

        // Deduplicate students by ID to prevent duplicate entries
        List<User> uniqueStudents = students.stream()
                .collect(java.util.stream.Collectors.toMap(
                        User::getId, u -> u, (a, b) -> a))
                .values().stream().toList();

        List<Map<String, Object>> result = new ArrayList<>();

        for (User student : uniqueStudents) {
            List<GradeReport> reports;

            if (subjects != null && !subjects.isEmpty()) {
                reports = subjects.stream()
                        .map(sub -> gradeReportRepository
                                .findByUserAndSubject(student, sub)
                                .orElse(null))
                        .filter(Objects::nonNull)
                        .toList();
            } else {
                reports = gradeReportRepository.findByUser(student);
            }

            double avgOverall = 0.0;
            if (!reports.isEmpty()) {
                double sum = reports.stream()
                        .mapToDouble(GradeService::calculateOverallGrade)
                        .sum();
                avgOverall = Math.round((sum / reports.size()) * 100.0) / 100.0;
            }

            double equivalence = getGradeEquivalence(avgOverall);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("studentId",    student.getId());
            entry.put("name",         student.getName());
            entry.put("email",        student.getEmail());
            entry.put("section",      student.getSection());
            entry.put("overallGrade", avgOverall);
            entry.put("equivalence",  equivalence);
            entry.put("score",        avgOverall);
            result.add(entry);
        }

        // Sort: equivalence ASC (1.0 best), then raw grade DESC as tiebreaker
        result.sort((a, b) -> {
            double eqA = (Double) a.get("equivalence");
            double eqB = (Double) b.get("equivalence");
            if (eqA != eqB) return Double.compare(eqA, eqB);
            return Double.compare((Double) b.get("overallGrade"), (Double) a.get("overallGrade"));
        });

        // Shared rank for same equivalence
        int rank = 1;
        for (int i = 0; i < result.size(); i++) {
            if (i > 0) {
                double prevEq = (Double) result.get(i - 1).get("equivalence");
                double currEq = (Double) result.get(i).get("equivalence");
                if (currEq != prevEq) rank = i + 1;
            }
            result.get(i).put("rank", rank);
        }

        return result;
    }

    // ── GRADE REPORT (used by website scores page) ────────────────────────────

    public GradeReport getGradeReport(User user, String subject) {
        return gradeReportRepository.findByUserAndSubject(user, subject)
                .orElseGet(() -> {
                    GradeReport newReport = new GradeReport(user, subject);
                    return gradeReportRepository.save(newReport);
                });
    }

    public Map<String, Object> getGradeReport(String email, String subject) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found: " + email));

        GradeReport report = getGradeReport(student, subject);

        // Recalculate using the correct formula before returning
        double overall = calculateOverallGrade(report);
        report.setOverallGrade(overall);
        gradeReportRepository.save(report);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("selfCheck1",   report.getSelfCheck1());
        result.put("selfCheck2",   report.getSelfCheck2());
        result.put("selfCheck3",   report.getSelfCheck3());
        result.put("selfCheck4",   report.getSelfCheck4());
        result.put("selfCheck5",   report.getSelfCheck5());
        result.put("taskSheet1",   report.getTaskSheet1());
        result.put("taskSheet2",   report.getTaskSheet2());
        result.put("taskSheet3",   report.getTaskSheet3());
        result.put("unitTest1",    report.getUnitTest1());
        result.put("unitTest2",    report.getUnitTest2());
        result.put("termTest",     report.getTermTest());
        result.put("attendance",   report.getAttendance());
        result.put("overallGrade", overall);
        return result;
    }

    // ── UPDATE GRADE (called by Flutter GradeController) ─────────────────────

    @org.springframework.transaction.annotation.Transactional
    public GradeReport updateGrade(Long userId, String subject,
                                   Map<String, Object> payload) {
        User student = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(
                        "Student not found with id: " + userId));

        GradeReport report = gradeReportRepository
                .findByUserAndSubject(student, subject)
                .orElse(new GradeReport(student, subject));

        report.setSelfCheck1(toInt(payload.get("selfCheck1")));
        report.setSelfCheck2(toInt(payload.get("selfCheck2")));
        report.setSelfCheck3(toInt(payload.get("selfCheck3")));
        report.setSelfCheck4(toInt(payload.get("selfCheck4")));
        report.setSelfCheck5(toInt(payload.get("selfCheck5")));
        report.setTaskSheet1(toInt(payload.get("taskSheet1")));
        report.setTaskSheet2(toInt(payload.get("taskSheet2")));
        report.setTaskSheet3(toInt(payload.get("taskSheet3")));
        report.setUnitTest1(toInt(payload.get("unitTest1")));
        report.setUnitTest2(toInt(payload.get("unitTest2")));
        report.setTermTest(toInt(payload.get("termTest")));
        report.setAttendance(toInt(payload.get("attendance")));

        // Always recalculate with the correct formula — ignore Flutter's value
        report.setOverallGrade(calculateOverallGrade(report));

        return gradeReportRepository.save(report);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int toInt(Object val) {
        if (val == null) return 0;
        return Integer.parseInt(val.toString());
    }
}