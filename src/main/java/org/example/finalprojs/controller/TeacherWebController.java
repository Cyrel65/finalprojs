package org.example.finalprojs.controller;

import org.example.finalprojs.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TeacherWebController {

    @Autowired
    private TeacherService teacherService;

    // 1. Show the reset page when they click the email link from Gmail
    @GetMapping("/teacher/reset-password")
    public String showResetPage(@RequestParam String token, Model model) {
        // We pass the token to the hidden field in the Thymeleaf HTML
        model.addAttribute("token", token);
        return "teacher-reset-password";
    }

    // 2. Handle the form submission from the web browser
    @PostMapping("/teacher/reset-password")
    public String handleReset(@RequestParam String token,
                              @RequestParam String newPassword,
                              @RequestParam String confirmPassword,
                              Model model) {

        // Validation check
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match!");
            model.addAttribute("token", token);
            return "teacher-reset-password";
        }

        try {
            // Updates the plain-text password in the database via TeacherService
            teacherService.updatePassword(token, newPassword);
            model.addAttribute("success", "Password updated successfully!");
        } catch (Exception e) {
            model.addAttribute("error", "Invalid or expired link.");
            model.addAttribute("token", token);
        }

        return "teacher-reset-password";
    }
}