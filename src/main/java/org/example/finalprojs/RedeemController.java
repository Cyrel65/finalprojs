package org.example.finalprojs;

import jakarta.servlet.http.HttpSession;
import org.example.finalprojs.model.Box;
import org.example.finalprojs.model.GradeReport; // New Dependency
import org.example.finalprojs.model.RedeemItem; // New Dependency
import org.example.finalprojs.model.RedeemTransaction;
import org.example.finalprojs.model.User;
import org.example.finalprojs.repository.BoxRepository;
import org.example.finalprojs.repository.GradeReportRepository; // New Dependency
import org.example.finalprojs.repository.RedeemItemRepository; // New Dependency
import org.example.finalprojs.repository.RedeemTransactionRepository;
import org.example.finalprojs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Class name changed from BoxController to RedeemController for clarity
@Controller
public class RedeemController {

    @Autowired
    private RedeemItemRepository redeemItemRepository;

    @Autowired
    private BoxRepository boxRepository;

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

    // --- 1. Dashboard GET Mapping (Standard) ---
    @GetMapping("/")
    public String viewDashboard(Model model) {
        List<Box> allBoxes = boxRepository.findAll();
        model.addAttribute("boxes", allBoxes);
        return "index";
    }

    // --- 2. Box Creation POST Mapping (Teacher functionality - assumes creating a Box) ---
    @PostMapping("/createBox")
    public String createNewBox(@RequestParam int points,
                               @RequestParam String typeOfTest,
                               RedirectAttributes redirectAttributes) {

        // NOTE: This should ideally be updated to save a RedeemItem if you stop using Box for rewards.
        User currentUser = userRepository.findById(CURRENT_USER_ID)
                .orElse(null);

        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "System error: Default user not found.");
            return "redirect:/login";
        }

        Box newBox = new Box(points, typeOfTest, currentUser);
        boxRepository.save(newBox);

        return "redirect:/redeem";
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
        User user = userOptional.get();

        // 1. Get current points from User
        model.addAttribute("currentPoints", user.getPoints());
        model.addAttribute("currentSubject", subjectName);

        // 2. Fetch Redeemable Items based on Subject
        if (subjectName == null || subjectName.isEmpty()) {
            // Display general error if subject is missing
            model.addAttribute("error", "Please select a subject to view rewards.");
            model.addAttribute("items", new ArrayList<RedeemItem>());
        } else {
            // Fetch items specific to the subject using the new Repository
            List<RedeemItem> items = redeemItemRepository.findBySubject(subjectName);
            model.addAttribute("items", items);
        }

        // NOTE: Renaming the return view from 'widgets' to 'redeem'
        return "redeem";
    }

    // --- 4. Redemption Execution POST Mapping (NEW LOGIC: Update GradeReport) ---
    @PostMapping("/redeem/execute")
    @Transactional
    public String executeRedeem(
            @RequestParam Long itemId, // Using itemId instead of boxId
            @RequestParam String subject, // NEW: Subject name required to find GradeReport
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOptional.get();

        // 1. Get the RedeemItem
        RedeemItem item = redeemItemRepository.findById(itemId)
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Reward item not found.");
                    return null;
                });

        if (item == null) {
            return "redirect:/redeem?subjectName=" + subject;
        }

        // 2. CRITICAL BUSINESS LOGIC: Check Points
        if (user.getPoints() < item.getCost()) { // Check against the item's cost
            redirectAttributes.addFlashAttribute("error",
                    "Redemption failed: Insufficient points. Cost: " + item.getCost() + ", Available: " + user.getPoints() + "."
            );
            return "redirect:/redeem?subjectName=" + subject;
        }

        // 3. Find the student's GradeReport for the subject
        GradeReport report = gradeReportRepository.findByUserAndSubject(user, subject)
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Grade report not found for subject: " + subject);
                    return null;
                });

        if (report == null) {
            return "redirect:/redeem?subjectName=" + subject;
        }

        try {
            // 4. Dynamically Update GradeReport field using Reflection
            String targetField = item.getTargetAssessment(); // e.g., "unitTest1"

            String capitalizedField = targetField.substring(0, 1).toUpperCase() + targetField.substring(1);
            String getterName = "get" + capitalizedField;
            String setterName = "set" + capitalizedField;

            // Get current score
            Method getter = GradeReport.class.getMethod(getterName);
            int currentScore = (Integer) getter.invoke(report);

            // Calculate new score
            int newScore = currentScore + item.getPointsAwarded();

            // Update the report object
            Method setter = GradeReport.class.getMethod(setterName, int.class);
            setter.invoke(report, newScore);

            // 5. Save the updated GradeReport and Deduct Points
            gradeReportRepository.save(report);

            int newPoints = user.getPoints() - item.getCost();
            user.setPoints(newPoints);
            userRepository.save(user);

            // 6. Record Transaction History
            RedeemTransaction transaction = new RedeemTransaction();
            // NOTE: Since RedeemTransaction uses Box, we'll use a placeholder Box to avoid breaking history.
            Box placeholderBox = boxRepository.findById(CURRENT_USER_ID).orElse(null);
            if (placeholderBox != null) {
                transaction.setBox(placeholderBox);
            }
            transaction.setUser(user);
            transaction.setRedeemDate(LocalDateTime.now());
            redeemTransactionRepository.save(transaction);

            redirectAttributes.addFlashAttribute("success",
                    "SUCCESS! " + item.getPointsAwarded() + " points added to your " + targetField + " in " + subject + ".");

        } catch (Exception e) {
            // The @Transactional annotation will roll back point deduction if grade update fails.
            redirectAttributes.addFlashAttribute("error", "Error processing reward: Failed to update grade (" + e.getMessage() + "). Points were not deducted.");
        }

        // Redirect to the updated scores page
        return "redirect:/scores?subjectName=" + subject;
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
}