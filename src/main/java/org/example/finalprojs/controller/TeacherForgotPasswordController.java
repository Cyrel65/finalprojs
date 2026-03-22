package org.example.finalprojs.controller;

import org.example.finalprojs.model.Teacher;
import org.example.finalprojs.service.EmailService;
import org.example.finalprojs.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher")
public class TeacherForgotPasswordController {

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private EmailService emailService;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> processTeacherForgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        Optional<Teacher> teacherOptional = teacherService.findTeacherByEmail(email);

        if (teacherOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Teacher account with this Gmail not found.");
        }

        Teacher teacher = teacherOptional.get();

        String token = UUID.randomUUID().toString();
        teacherService.updateResetToken(teacher.getId(), token);

        // UPDATED: Using your specific IP address for the link
        String resetLink = "http://192.168.18.17:8080/teacher/reset-password?token=" + token;

        try {
            emailService.sendPasswordResetEmail(email, resetLink);
            return ResponseEntity.ok("Reset link sent successfully to " + email);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send email. Check your SMTP settings.");
        }
    }
}