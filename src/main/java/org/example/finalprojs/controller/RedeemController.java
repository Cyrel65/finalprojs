package org.example.finalprojs.controller;

import org.example.finalprojs.model.RedeemTransaction;
import org.example.finalprojs.model.User;
import org.example.finalprojs.model.RedeemItem;
import org.example.finalprojs.service.MessageService;
import org.example.finalprojs.service.RedeemService;
import org.example.finalprojs.service.UserService;
import org.example.finalprojs.service.ClassService;

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

    private final UserService userService;
    private final MessageService messageService;
    private final RedeemService redeemService;
    private final ClassService classService;

    @Autowired
    public RedeemController(UserService userService,
                            MessageService messageService,
                            RedeemService redeemService,
                            ClassService classService) {
        this.userService    = userService;
        this.messageService = messageService;
        this.redeemService  = redeemService;
        this.classService   = classService;
    }

    private Optional<User> getCurrentUser(HttpSession session) {
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) return Optional.empty();
        return userService.findUserByEmail(userEmail);
    }

    @GetMapping("/redeem")
    public String viewRedeemPage(
            @RequestParam(required = false) String subjectName,
            @RequestParam(required = false) String section,
            Model model,
            HttpSession session) {

        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) return "redirect:/login";

        User currentUser = userOptional.get();

        model.addAttribute("user",           currentUser);
        model.addAttribute("unreadCount",    messageService.getUnreadCount(currentUser));
        model.addAttribute("sentCount",      messageService.getSentCount(currentUser));
        model.addAttribute("currentSubject", subjectName);
        model.addAttribute("currentSection", section);

        // Per-subject+section points balance
        int subjectPoints = 0;
        if (subjectName != null && !subjectName.isEmpty()
                && section != null && !section.isEmpty()) {
            subjectPoints = redeemService.getPointsForSubjectAndSection(
                    currentUser, subjectName, section);
        }
        model.addAttribute("currentPoints", subjectPoints);

        // Transaction history
        try {
            List<RedeemTransaction> transactions =
                    redeemService.findTransactionsByUser(currentUser);
            model.addAttribute("transactions", transactions);
        } catch (Exception e) {
            model.addAttribute("transactions", new ArrayList<RedeemTransaction>());
        }

        // UPDATED: fetch rewards for this subject AND section only
        if (subjectName == null || subjectName.isEmpty()) {
            model.addAttribute("error", "Please select a subject to view rewards.");
            model.addAttribute("items", new ArrayList<RedeemItem>());
        } else if (section == null || section.isEmpty()) {
            model.addAttribute("error", "Section not found. Please return to the dashboard.");
            model.addAttribute("items", new ArrayList<RedeemItem>());
        } else {
            // Only rewards created for THIS subject + THIS section are returned
            List<RedeemItem> items =
                    redeemService.findRedeemItemsBySubjectAndSection(subjectName, section);
            model.addAttribute("items", items);
        }

        model.addAttribute("pageTitle", "Redeem Rewards");
        return "redeem";
    }

    @PostMapping("/redeem/execute")
    public String executeRedeem(
            @RequestParam Long redeemItemId,
            @RequestParam(required = false) String section,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) return "redirect:/login";

        User currentUser       = userOptional.get();
        String redirectSubject = "";
        String redirectSection = section != null ? section : "";

        try {
            Optional<RedeemItem> itemOptional = redeemService.findRedeemItemById(redeemItemId);
            if (itemOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "The selected reward is invalid.");
                return "redirect:/redeem";
            }

            RedeemItem item = itemOptional.get();
            redirectSubject = item.getSubject();

            redeemService.executeRedemption(currentUser, redeemItemId, redirectSection);

            redirectAttributes.addFlashAttribute("success",
                    "🎉 " + item.getRewardName() + " redeemed successfully! +"
                            + item.getPointsAwarded() + " pts added to "
                            + item.getTargetAssessment() + ".");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "❌ " + e.getMessage());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error",
                    "❌ An unexpected error occurred during redemption.");
        }

        return "redirect:/redeem?subjectName=" + redirectSubject
                + "&section=" + redirectSection;
    }
}