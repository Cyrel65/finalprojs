package org.example.finalprojs;

import org.example.finalprojs.model.User;
import org.example.finalprojs.model.Message;
import org.example.finalprojs.model.GradeReport;
import org.example.finalprojs.repository.GradeReportRepository;
import org.example.finalprojs.repository.BoxRepository;
import org.example.finalprojs.repository.MessageRepository;
import org.example.finalprojs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Standard Java Utility Imports (Fixes "Cannot resolve symbol" errors)
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.security.Principal; // Not strictly used, but kept from original structure


@Controller
public class controller {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BoxRepository boxRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private GradeReportRepository gradeReportRepository;


    // Private Helper Method for Session User
    private Optional<User> getCurrentUser(HttpSession session) {
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(userEmail);
    }

    // --- Registration Logic ---

    @GetMapping("/register")
    public String viewRegister(Model model) {
        model.addAttribute("user", new User());
        return "page-register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user) {
        userRepository.save(user);
        return "redirect:/login";
    }

    // --- Authentication Handlers ---

    @GetMapping("/login")
    public String viewLogin() {
        return "page-login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String email,
                            @RequestParam String password,
                            Model model,
                            HttpSession session) {

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            model.addAttribute("loginError", "Login failed: Email not found.");
            return "page-login";
        }

        User user = userOptional.get();

        if (user.getPassword().equals(password)) {
            session.setAttribute("userEmail", user.getEmail());
            return "redirect:/";
        } else {
            model.addAttribute("loginError", "Login failed: Incorrect password.");
            return "page-login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }


    @GetMapping("/profile")
    public String viewProfile(Model model, HttpSession session) {
        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("user", userOptional.get());
        return "app-profile";
    }

    // Unified method for updating profile picture
    @PostMapping("/update/profile-picture")
    public String updateProfilePicture(@RequestParam String profilePictureUrl, HttpSession session, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/";
        }

        try {
            User user = userOptional.get();
            user.setProfilePictureUrl(profilePictureUrl);
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("success", "Profile picture updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update profile picture: " + e.getMessage());
        }

        return "redirect:/profile";
    }



    // --- Message/Email Handlers ---

    @GetMapping("/inbox")
    public String viewInbox() {
        return "email-inbox";
    }

    @GetMapping("/read")
    public String viewRead() {
        return "email-read";
    }

    @GetMapping("/compose")
    public String viewCompose(HttpSession session) {
        if (getCurrentUser(session).isEmpty()) {
            return "redirect:/login";
        }
        return "email-compose";
    }

    @PostMapping("/send-message")
    public String sendMessage(
            @RequestParam("recipientEmail") String recipientEmail,
            @RequestParam("content") String content,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Optional<User> senderOptional = getCurrentUser(session);
        if (senderOptional.isEmpty()) {
            return "redirect:/login";
        }
        User sender = senderOptional.get();

        if (content == null || content.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Message content cannot be empty.");
            return "redirect:/compose";
        }

        try {
            User recipient = userRepository.findByEmail(recipientEmail)
                    .orElseThrow(() -> new RuntimeException("Recipient not found with email: " + recipientEmail));

            if (sender.getEmail().equalsIgnoreCase(recipientEmail)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cannot send message to yourself.");
                return "redirect:/compose";
            }

            Message message = new Message(
                    sender,
                    recipient,
                    content,
                    LocalDateTime.now()
            );
            messageRepository.save(message);

            redirectAttributes.addFlashAttribute("successMessage", "Message sent successfully!");
            return "redirect:/inbox";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error sending message: " + e.getMessage());
            return "redirect:/compose";
        }
    }

    // --- Personalized Grade Report Handler ---

    @GetMapping({"/scores", "/table"})
    public String viewScores(
            @RequestParam(required = false) String subjectName,
            Model model,
            HttpSession session) {

        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOptional.get();

        model.addAttribute("currentSubject", subjectName != null ? subjectName : "All Subjects");
        model.addAttribute("user", user);

        // 1. Fetch the single GradeReport for the user and subject
        Optional<GradeReport> reportOptional = gradeReportRepository.findByUserAndSubject(user, subjectName);

        GradeReport report;

        if (reportOptional.isEmpty()) {
            // If no report found, create an empty one for Thymeleaf to display zeros/dashes
            report = new GradeReport();
            report.setSubject(subjectName);
            report.setUser(user);
            // Defaulting overall grade to 10% (Attendance) if all other scores are zero
            report.setOverallGrade(0.0);
        } else {
            report = reportOptional.get();
            // 2. Calculate the overall grade based on the raw points fetched
            double calculatedOverall = calculateOverallGrade(report);
            report.setOverallGrade(calculatedOverall);
        }

        // 3. Pass the full report object to the model
        model.addAttribute("report", report);

        return "scores";
    }


    // --- Grade Calculation Logic ---

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

    //  --- Other Views ---

    @GetMapping("/viewclass")
    public String viewClass() {return "viewclasses";}


    @GetMapping("/forgotpass")
    public String viewPassword() {return "forgot-password";}

    @GetMapping("/helpFeed")
    public String viewHelpFeed() {return "help-feedback";}

    @GetMapping("/newindex")
    public String newindex() {return "index";}

    @GetMapping("/about")
    public String About() {return "about";}


    @GetMapping("/records")
    public String Record() {return "records";}

    @GetMapping("/section")
    public String Section() {return "table-datatable";}

}