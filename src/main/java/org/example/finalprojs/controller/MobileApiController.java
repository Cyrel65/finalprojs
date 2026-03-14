package org.example.finalprojs.controller;

import org.example.finalprojs.model.*;
import org.example.finalprojs.repository.GradeReportRepository;
import org.example.finalprojs.service.ClassService;
import org.example.finalprojs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mobile")
public class MobileApiController {

    @Autowired private ClassService classService;
    @Autowired private GradeReportRepository gradeReportRepository;
    @Autowired private UserRepository userRepository;

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
    public ResponseEntity<GradeReport> getDetails(@RequestParam String email, @RequestParam String subject) {
        User student = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(gradeReportRepository.findByUserAndSubject(student, subject).orElse(new GradeReport()));
    }
}