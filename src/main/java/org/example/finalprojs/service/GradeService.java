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
        this.gradeReportRepository = gradeReportRepository;
        this.teacherClassRepository = teacherClassRepository;
        this.userRepository = userRepository;
    }

    public List<Map<String, Object>> getRankingsBySection(String section) {
        var classes = teacherClassRepository.findBySection(section);
        if (classes.isEmpty()) return Collections.emptyList();

        String subject = classes.get(0).getSubject();
        List<GradeReport> reports = gradeReportRepository.findBySubject(subject);

        List<Map<String, Object>> result = new ArrayList<>();
        for (GradeReport report : reports) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("studentName",  report.getUser().getName());
            entry.put("studentEmail", report.getUser().getEmail());
            entry.put("overallGrade", report.getOverallGrade());
            result.add(entry);
        }

        result.sort((a, b) -> Double.compare(
                (Double) b.get("overallGrade"),
                (Double) a.get("overallGrade")
        ));

        return result;
    }

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
        result.put("overallGrade", report.getOverallGrade());
        return result;
    }
}