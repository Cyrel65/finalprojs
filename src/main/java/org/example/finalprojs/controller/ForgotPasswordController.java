package org.example.finalprojs.controller;

import org.example.finalprojs.model.User;
import org.example.finalprojs.service.EmailService;
import org.example.finalprojs.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class ForgotPasswordController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    // Show forgot password page
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    // Handle submission
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, Model model) {

        Optional<User> optional = userService.findUserByEmail(email);

        if (optional.isEmpty()) {
            model.addAttribute("error", "Email not found.");
            return "forgot-password";
        }

        User user = optional.get();
        String token = userService.createPasswordResetToken(user);

        String resetLink = "http://localhost:8080/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(email, resetLink);

        model.addAttribute("message", "A password reset link has been sent to your email.");
        return "forgot-password";
    }

    // Show reset password page
    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam String token, Model model) {

        Optional<User> optional = userService.validatePasswordResetToken(token);

        if (optional.isEmpty()) {
            model.addAttribute("error", "Invalid or expired token.");
            return "reset-password";
        }

        model.addAttribute("token", token);
        return "reset-password";
    }

    // Handle reset password form
    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model) {

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("token", token);
            return "reset-password";
        }

        try {
            userService.updatePassword(token, newPassword);

            // SUCCESS FLAG
            model.addAttribute("success", "You have successfully changed your password.");

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("token", token);
            return "reset-password";
        }

        // Keep the token in case user wants to retry
        model.addAttribute("token", token);

        return "reset-password"; // Stay on the same page and show success
    }
}
