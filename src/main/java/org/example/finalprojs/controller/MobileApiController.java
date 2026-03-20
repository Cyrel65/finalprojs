package org.example.finalprojs.controller;

import org.example.finalprojs.model.*;
import org.example.finalprojs.repository.GradeReportRepository;
import org.example.finalprojs.repository.StudentPointsRepository;
import org.example.finalprojs.repository.UserRepository;
import org.example.finalprojs.service.ClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mobile")
@CrossOrigin(origins = "*")
public class MobileApiController {

    @Autowired private ClassService classService;
    @Autowired private GradeReportRepository gradeReportRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentPointsRepository studentPointsRepository;

    // ─── EXISTING endpoints (unchanged) ──────────────────────────────────────

    @PostMapping("/scores/add")
    public ResponseEntity<?> addScore(@RequestParam String email,
                                      @RequestParam String subject,
                                      @RequestParam String assessmentName,
                                      @RequestParam int points) {
        classService.updateAssessmentScore(email, subject, assessmentName, points);
        return ResponseEntity.ok("Points added and reward system updated");
    }

    @PostMapping("/scores/reset")
    public ResponseEntity<?> resetScore(@RequestParam String email,
                                        @RequestParam String subject,
                                        @RequestParam String assessmentName) {
        classService.resetAssessmentScore(email, subject, assessmentName);
        return ResponseEntity.ok("Score cleared");
    }

    @GetMapping("/scores/details")
    public ResponseEntity<GradeReport> getDetails(@RequestParam String email,
                                                  @RequestParam String subject) {
        User student = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(
                gradeReportRepository.findByUserAndSubject(student, subject)
                        .orElse(new GradeReport())
        );
    }

    // ─── NEW: Add per-subject points to a student's balance ──────────────────
    // Points are stored per subject+section, NOT globally on User.points.
    // Students can only spend these points on rewards for the same subject.

    @PostMapping("/points/add")
    public ResponseEntity<?> addUniversalPoints(@RequestParam String email,
                                                @RequestParam String subject,
                                                @RequestParam String section,
                                                @RequestParam int points) {
        try {
            if (points <= 0) {
                return ResponseEntity.badRequest()
                        .body("Points must be greater than 0");
            }

            User student = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Student not found: " + email));

            // Find existing balance for this subject+section or create a new one
            StudentPoints balance = studentPointsRepository
                    .findByUserAndSubjectAndSection(student, subject, section)
                    .orElse(new StudentPoints(student, subject, section));

            balance.setPoints(balance.getPoints() + points);
            studentPointsRepository.save(balance);

            return ResponseEntity.ok(Map.of(
                    "message", "Added " + points + " points to " + student.getName()
                            + " for " + subject + " - " + section,
                    "newBalance", balance.getPoints(),
                    "subject", subject,
                    "section", section
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(500)
                    .body("Failed to add points: " + e.getMessage());
        }
    }
}