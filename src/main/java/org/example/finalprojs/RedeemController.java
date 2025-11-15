package org.example.finalprojs;

import org.example.finalprojs.repository.MessageRepository;
import jakarta.servlet.http.HttpSession;
import org.example.finalprojs.model.GradeReport; // New Dependency
import org.example.finalprojs.model.RedeemItem; // New Dependency
import org.example.finalprojs.model.RedeemTransaction;
import org.example.finalprojs.model.User;
import org.example.finalprojs.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;
import org.example.finalprojs.model.Message; // <-- ADD THIS LINE


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Class name changed from BoxController to RedeemController for clarity
@Controller
public class RedeemController {

    @Autowired
    private RedeemItemRepository redeemItemRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedeemTransactionRepository redeemTransactionRepository;

    @Autowired
    private GradeReportRepository gradeReportRepository; // NEW: Dependency for grade updates

    // Placeholder for the current user's ID (Teacher access assumed)
    private static final Long CURRENT_USER_ID = 1L;

    // Helper method
    private Optional<User> getCurrentUser(HttpSession session) {
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(userEmail);
    }





    // --- 3. Dynamic Redeem Page GET Mapping (Subject-specific) ---
    @GetMapping("/redeem")
    public String viewRedeemPage(
            @RequestParam(required = false) String subjectName,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = userOptional.get();

        // FIX 1: Add currentUser object for the header/sidebar profile picture
        model.addAttribute("user", currentUser);

        // FIX 2: Add Message Counts for the header/sidebar badges
        List<Message> receivedMessages = messageRepository.findByRecipient(currentUser);
        long unreadCount = receivedMessages.stream().filter(m -> !m.isRead()).count();
        model.addAttribute("unreadCount", unreadCount);

        long sentCount = messageRepository.countBySender(currentUser);
        model.addAttribute("sentCount", sentCount);

        // 1. Get current points from User (using the variable assigned above)
        model.addAttribute("currentPoints", currentUser.getPoints());
        model.addAttribute("currentSubject", subjectName);

        // 2. Fetch Redeemable Items based on Subject (Your original logic)
        if (subjectName == null || subjectName.isEmpty()) {
            // Display general error if subject is missing
            model.addAttribute("error", "Please select a subject to view rewards.");
            model.addAttribute("items", new ArrayList<RedeemItem>());
        } else {
            // Fetch items specific to the subject using the new Repository
            List<RedeemItem> items = redeemItemRepository.findBySubject(subjectName);
            model.addAttribute("items", items);
        }

        // Set page title
        model.addAttribute("pageTitle", "Redeem Rewards");

        // NOTE: Renaming the return view from 'widgets' to 'redeem'
        return "redeem";
    }

    // In RedeemController.java

    // --- 4. Redemption Execution POST Mapping (FIXED: Point deduction moved) ---
    @PostMapping("/redeem/execute")
    @Transactional
    public String executeRedeem(
            @RequestParam Long itemId,
            @RequestParam String subject,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOptional.get();

        RedeemItem item = redeemItemRepository.findById(itemId)
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Reward item not found.");
                    return null;
                });

        if (item == null) {
            return "redirect:/redeem?subjectName=" + subject + "&t=" + System.currentTimeMillis();
        }

        // 1. Check Cost
        if (user.getPoints() < item.getCost()) {
            redirectAttributes.addFlashAttribute("error",
                    "Redemption failed: Insufficient points. Cost: " + item.getCost() + ", Available: " + user.getPoints() + "."
            );
            return "redirect:/redeem?subjectName=" + subject + "&t=" + System.currentTimeMillis();
        }

        // 2. Find the student's GradeReport for the subject
        GradeReport report = gradeReportRepository.findByUserAndSubject(user, subject)
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Grade report not found for subject: " + subject);
                    return null;
                });

        if (report == null) {
            return "redirect:/redeem?subjectName=" + subject + "&t=" + System.currentTimeMillis();
        }

        try {
            // --- 3. DYNAMIC GRADE UPDATE SETUP ---
            String targetField = item.getTargetAssessment();
            String capitalizedField = targetField.substring(0, 1).toUpperCase() + targetField.substring(1);
            String getterName = "get" + capitalizedField;
            String setterName = "set" + capitalizedField;

            // Use Reflection to get current score
            Method getter = GradeReport.class.getMethod(getterName);
            int currentScore = (Integer) getter.invoke(report);

            int pointsAwarded = item.getPointsAwarded();
            int newScore = currentScore + pointsAwarded;

            // --- 4. MAX SCORE CHECK ---
            int maxScore = getMaxScoreForAssessment(targetField);

            if (newScore > maxScore) {
                int difference = newScore - maxScore;

                // Prepare the HTML error message
                String alertMessage = "<div class=\"alert alert-danger\">Redemption failed: Cannot exceed the maximum score for " + targetField + ". Max score is " + maxScore + ". This redemption would go over by " + difference + " points.</div>";

                redirectAttributes.addFlashAttribute("errorMessage", alertMessage);

                // CRITICAL: Exit here before points are deducted
                return "redirect:/redeem?subjectName=" + subject + "&t=" + System.currentTimeMillis();
            }
            // ---------------------------------------------------

            // --- 5. SUCCESSFUL TRANSACTION (ONLY EXECUTED IF MAX SCORE CHECK PASSES) ---

            // A. Update the report object
            Method setter = GradeReport.class.getMethod(setterName, int.class);
            setter.invoke(report, newScore);
            gradeReportRepository.save(report);

            // B. Deduct Points
            int newPoints = user.getPoints() - item.getCost();
            user.setPoints(newPoints);
            userRepository.save(user);

            // C. Record Transaction History (existing logic)
            // You should put your transaction history saving logic here
            // ...
            // redeemTransactionRepository.save(new RedeemTransaction(...));
            // ...

            // D. Prepare Success Message
            String successMessageContent = "SUCCESS! " + item.getPointsAwarded() + " points added to your " + targetField + " in " + subject + ".";
            String successAlertHtml = "<div class=\"alert alert-success\">" + successMessageContent + "</div>";
            redirectAttributes.addFlashAttribute("successHtml", successAlertHtml);

            // E. Redirect to Scores Page
            return "redirect:/scores?subjectName=" + subject + "&t=" + System.currentTimeMillis();

        } catch (Exception e) {
            // If any reflection/database error occurs, points were not deducted (due to @Transactional)
            redirectAttributes.addFlashAttribute("error", "Error processing reward: An internal system error occurred. Points were not deducted.");
            return "redirect:/redeem?subjectName=" + subject + "&t=" + System.currentTimeMillis();
        }
    }

    // --- 5. Redeem History GET Mapping (Standard) ---
    @GetMapping("/redeem/history")
    public String viewRedeemHistory(Model model, HttpSession session) {

        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOptional.get();

        List<RedeemTransaction> history = redeemTransactionRepository.findByUserOrderByRedeemDateDesc(user);

        model.addAttribute("history", history);

        return "redeem_history";

    }

    // In RedeemController.java

    /**
     * Helper method to return the static maximum score for a given assessment type.
     * This should match the max scores shown in your scores table.
     */
    private int getMaxScoreForAssessment(String assessmentType) {
        // Standardizing the max scores based on your scores table structure (Max 10, Max 20, Max 50)

        // NOTE: If your max scores vary, you might need a dedicated database table for max weights/scores.

        // Self Checks (SC 1-5) are max 10
        if (assessmentType.startsWith("selfCheck")) {
            return 10;
        }
        // Task Sheets (TS 1-3) are max 20
        else if (assessmentType.startsWith("taskSheet")) {
            return 20;
        }
        // Unit Tests (UT 1-2) and Term Test are max 50
        else if (assessmentType.startsWith("unitTest") || assessmentType.equals("termTest")) {
            return 50;
        }
        // Attendance is handled separately (usually 100%)
        else if (assessmentType.equals("attendance")) {
            return 100;
        }
        // Default or unknown assessment
        return 0;
    }
}