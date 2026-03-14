package org.example.finalprojs.service;

import org.example.finalprojs.model.*;
import org.example.finalprojs.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClassService {

    private final TeacherClassRepository teacherClassRepository;
    private final GradeReportRepository gradeReportRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;

    @Autowired
    public ClassService(TeacherClassRepository teacherClassRepository,
                        GradeReportRepository gradeReportRepository,
                        UserRepository userRepository,
                        TeacherRepository teacherRepository) {
        this.teacherClassRepository = teacherClassRepository;
        this.gradeReportRepository  = gradeReportRepository;
        this.userRepository         = userRepository;
        this.teacherRepository      = teacherRepository;
    }

    public TeacherClass createClass(Long teacherId, String subject, String section, String startTime) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found with ID: " + teacherId));

        TeacherClass newClass = new TeacherClass();
        newClass.setTeacher(teacher);
        newClass.setSubject(subject);
        newClass.setSection(section);
        newClass.setStartTime(startTime);

        return teacherClassRepository.save(newClass);
    }

    public TeacherClass updateClass(Long classId, String subject, String section, String startTime) {
        TeacherClass tc = teacherClassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        tc.setSubject(subject);
        tc.setSection(section);
        tc.setStartTime(startTime);
        return teacherClassRepository.save(tc);
    }

    /**
     * Adds points to one assessment field for a student.
     * Looks up the student by email, then finds-or-creates their GradeReport
     * for the given subject. Uses the fixed single GradeReportRepository so
     * findByUserAndSubject() resolves correctly.
     */
    public void updateAssessmentScore(String email, String subject, String assessmentName, int points) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found: " + email));

        // findByUserAndSubject now resolves to the single correct interface
        GradeReport report = gradeReportRepository
                .findByUserAndSubject(student, subject)
                .orElse(new GradeReport(student, subject));

        report.addPointsToAssessment(assessmentName, points);

        // Award the student's total points tally
        student.setPoints(student.getPoints() + points);
        userRepository.save(student);

        gradeReportRepository.save(report);
    }

    /**
     * Resets one assessment field to 0 for a student.
     */
    public void resetAssessmentScore(String email, String subject, String assessmentName) {
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found: " + email));

        GradeReport report = gradeReportRepository
                .findByUserAndSubject(student, subject)
                .orElseThrow(() -> new RuntimeException("Grade report not found for: " + email));

        switch (assessmentName) {
            case "selfCheck1": report.setSelfCheck1(0); break;
            case "selfCheck2": report.setSelfCheck2(0); break;
            case "selfCheck3": report.setSelfCheck3(0); break;
            case "selfCheck4": report.setSelfCheck4(0); break;
            case "selfCheck5": report.setSelfCheck5(0); break;
            case "taskSheet1": report.setTaskSheet1(0); break;
            case "taskSheet2": report.setTaskSheet2(0); break;
            case "taskSheet3": report.setTaskSheet3(0); break;
            case "unitTest1":  report.setUnitTest1(0);  break;
            case "unitTest2":  report.setUnitTest2(0);  break;
            case "termTest":   report.setTermTest(0);   break;
            case "attendance": report.setAttendance(0); break;
        }
        gradeReportRepository.save(report);
    }

    /**
     * Deletes a class and all its associated grade reports.
     */
    @Transactional
    public void deleteClass(Long classId) {
        TeacherClass tc = teacherClassRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        // deleteBySubject resolves correctly from the single GradeReportRepository
        gradeReportRepository.deleteBySubject(tc.getSubject());
        teacherClassRepository.deleteById(classId);
    }

    public List<TeacherClass> getClassesByTeacher(Long teacherId) {
        return teacherClassRepository.findByTeacherId(teacherId);
    }

    public List<TeacherClass> getClassesBySection(String section) {
        return teacherClassRepository.findBySection(section);
    }
}