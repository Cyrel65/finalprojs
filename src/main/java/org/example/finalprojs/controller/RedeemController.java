package org.example.finalprojs.controller;

import org.example.finalprojs.model.RedeemTransaction;
import org.example.finalprojs.model.User;
import org.example.finalprojs.model.RedeemItem;
import org.example.finalprojs.service.MessageService; // Dependency for counts
import org.example.finalprojs.service.RedeemService; // New Service
import org.example.finalprojs.service.UserService; // Dependency for auth lookup

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class RedeemController {

    // Note: We are injecting the Services we need, not the Repositories.
    private final UserService userService;
    private final MessageService messageService;
    private final RedeemService redeemService;

    @Autowired
    public RedeemController(UserService userService,
                            MessageService messageService,
                            RedeemService redeemService) {
        this.userService = userService;
        this.messageService = messageService;
        this.redeemService = redeemService;
    }

    // --- Helper Method: Match Auth Logic from other Controllers (Uses UserService) ---
    private Optional<User> getCurrentUser(HttpSession session) {
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) {
            return Optional.empty();
        }
        return userService.findUserByEmail(userEmail);
    }

    // --- GET Mapping for Redeem Page ---
    @GetMapping("/redeem")
    public String viewRedeemPage(
            @RequestParam(required = false) String subjectName,
            Model model,
            HttpSession session) {

        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = userOptional.get();

        // Add user and counts (using MessageService for counts)
        model.addAttribute("user", currentUser);
        model.addAttribute("unreadCount", messageService.getUnreadCount(currentUser));
        model.addAttribute("sentCount", messageService.getSentCount(currentUser));

        model.addAttribute("currentPoints", currentUser.getPoints());
        model.addAttribute("currentSubject", subjectName);

        // --- ADD THIS BLOCK TO LOAD TRANSACTION DATA FOR THE MODAL ---
        try {
            List<RedeemTransaction> transactions = redeemService.findTransactionsByUser(currentUser);
            model.addAttribute("transactions", transactions);
        } catch (Exception e) {
            // Handle error case by sending an empty list
            model.addAttribute("transactions", new ArrayList<RedeemTransaction>());
            System.err.println("Error fetching transactions for modal: " + e.getMessage());
        }
        // -----------------------------------------------------------

        if (subjectName == null || subjectName.isEmpty()) {
            model.addAttribute("error", "Please select a subject to view rewards.");
            model.addAttribute("items", new ArrayList<RedeemItem>());
        } else {
            // Delegate item fetching to RedeemService
            List<RedeemItem> items = redeemService.findRedeemItemsBySubject(subjectName);
            model.addAttribute("items", items);
        }

        model.addAttribute("pageTitle", "Redeem Rewards");
        return "redeem";
    }


    // --- POST Mapping to EXECUTE REDEMPTION (Delegated) ---
    @PostMapping("/redeem/execute")
    public String executeRedeem(
            @RequestParam Long redeemItemId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = userOptional.get();

        String redirectSubject = ""; // Subject to redirect back to, determined below

        try {
            Optional<RedeemItem> itemOptional = redeemService.findRedeemItemById(redeemItemId);
            if (itemOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "The selected reward is invalid.");
                return "redirect:/redeem";
            }
            RedeemItem item = itemOptional.get();
            redirectSubject = item.getSubject(); // Set subject for redirect

            // Delegate the entire transaction logic to the service
            redeemService.executeRedemption(currentUser, redeemItemId);

            redirectAttributes.addFlashAttribute("success",
                    item.getRewardName() + " redeemed successfully! Points applied to your " + item.getSubject() + " report.");

        } catch (IllegalArgumentException e) {
            // Catch validation failures thrown by the service
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (RuntimeException e) {
            // Fallback for unexpected errors
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred during redemption.");
        }

        // Redirect back to the subject-specific redeem page
        return "redirect:/redeem?subjectName=" + redirectSubject;
    }
}