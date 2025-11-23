package org.example.finalprojs.controller;

import org.example.finalprojs.model.User;
import org.example.finalprojs.service.MessageService;
import org.example.finalprojs.service.FeedbackService;
import org.example.finalprojs.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;

@Controller
@RequestMapping
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final MessageService messageService;
    private final UserService userService;

    @Autowired
    public FeedbackController(FeedbackService feedbackService, MessageService messageService, UserService userService) {
        this.feedbackService = feedbackService;
        this.messageService = messageService;
        this.userService = userService;
    }

    /**
     * Helper method to retrieve the current user from the session.
     * Authenticates by looking up the user via email stored in the session.
     */
    private Optional<User> getCurrentUser(HttpSession session) {
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) {
            return Optional.empty();
        }
        return userService.findUserByEmail(userEmail);
    }

    @GetMapping("/helpFeed")
    public String viewHelpFeed(Model model, HttpSession session) {
        // Authenticate and check user session
        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login"; // Redirect if not logged in
        }

        User currentUser = userOptional.get();

        // Add required data to the model
        model.addAttribute("user", currentUser);
        model.addAttribute("unreadCount", messageService.getUnreadCount(currentUser));

        return "help-feedback"; // Return your original view name
    }

    @PostMapping("/submitFeedback")
    public String submitFeedback(
            @RequestParam("comment") String commentText,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        //  Authenticate and check user session
        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }

        try {
            User user = userOptional.get();
            Long userId = user.getId(); // Assuming User has a getId() method

            //  Call the service to submit the feedback
            feedbackService.submitFeedback(userId, commentText);

            redirectAttributes.addFlashAttribute("success", "Thank you for your feedback! We appreciate you taking the time to write to us.");
        } catch (IllegalArgumentException e) {
            // Handles validation errors
            redirectAttributes.addFlashAttribute("error", "Submission failed: " + e.getMessage());
        } catch (Exception e) {
            // General error handling
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred during submission.");
        }

        // Redirects back to the GET /helpFeed endpoint to show status messages
        return "redirect:/helpFeed";
    }
}