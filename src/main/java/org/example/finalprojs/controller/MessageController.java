package org.example.finalprojs.controller;

import jakarta.servlet.http.HttpSession;
import org.example.finalprojs.model.Message;
import org.example.finalprojs.model.User;
import org.example.finalprojs.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class MessageController {

    private final MessageService messageService;

    // Use constructor injection
    @Autowired
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }


    // Private Helper Method for Session User (Uses service for lookup)
    private Optional<User> getCurrentUser(HttpSession session) {
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) {
            return Optional.empty();
        }
        return messageService.findUserByEmail(userEmail);
    }

    // --- Dashboard Handler ---

    @GetMapping("/")
    public String viewDashboard(Model model, HttpSession session) {
        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = userOptional.get();

        model.addAttribute("user", currentUser);
        model.addAttribute("unreadCount", messageService.getUnreadCount(currentUser));
        model.addAttribute("sentCount", messageService.getSentCount(currentUser));
        model.addAttribute("pageTitle", "Dashboard");

        return "index";
    }

    // --- Message/Email Handlers ---

    @GetMapping("/inbox")
    public String viewInbox(Model model, HttpSession session) {
        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = userOptional.get();

        // Delegate retrieval to service
        List<Message> receivedMessages = messageService.getInboxMessages(currentUser);

        // Add to model
        model.addAttribute("user", currentUser);
        model.addAttribute("messages", receivedMessages);
        model.addAttribute("unreadCount", messageService.getUnreadCount(currentUser));
        model.addAttribute("sentCount", messageService.getSentCount(currentUser));
        model.addAttribute("pageTitle", "Inbox");

        return "email-inbox";
    }

    @GetMapping("/sent")
    public String sentMessages(Model model, HttpSession session) {
        Optional<User> currentUserOptional = getCurrentUser(session);
        if (currentUserOptional.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = currentUserOptional.get();

        // Delegate retrieval to service
        List<Message> sentMessages = messageService.getSentMessages(currentUser);

        // Add to model
        model.addAttribute("user", currentUser);
        model.addAttribute("messages", sentMessages);
        model.addAttribute("sentCount", messageService.getSentCount(currentUser));
        model.addAttribute("unreadCount", messageService.getUnreadCount(currentUser));
        model.addAttribute("pageTitle", "Sent Messages");

        return "email-inbox";
    }

    @GetMapping("/read")
    public String viewRead(@RequestParam Long messageId, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = userOptional.get();

        Optional<Message> messageOptional = messageService.getMessageById(messageId);
        if (messageOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Message not found.");
            return "redirect:/inbox";
        }

        Message message = messageOptional.get();

        // Security Check: Delegate access validation to the service
        if (!messageService.userCanAccessMessage(currentUser, message)) {
            redirectAttributes.addFlashAttribute("error", "Access denied. This message is not yours.");
            return "redirect:/inbox";
        }

        // Mark as Read Logic: Delegate to the service
        messageService.markMessageAsRead(message, currentUser);

        // Add to model
        model.addAttribute("user", currentUser);
        model.addAttribute("message", message);
        model.addAttribute("unreadCount", messageService.getUnreadCount(currentUser));
        model.addAttribute("sentCount", messageService.getSentCount(currentUser));

        return "email-read";
    }

    @GetMapping("/compose")
    public String viewCompose(Model model, HttpSession session) {
        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = userOptional.get();

        // Delegate user list retrieval to service
        List<User> allUsers = messageService.getAllUsers();

        // Add to model
        model.addAttribute("user", currentUser);
        model.addAttribute("unreadCount", messageService.getUnreadCount(currentUser));
        model.addAttribute("sentCount", messageService.getSentCount(currentUser));
        model.addAttribute("allUsers", allUsers);
        model.addAttribute("pageTitle", "Compose Message");

        return "email-compose";
    }

    @PostMapping("/send-message")
    public String sendMessage(
            @RequestParam("recipientEmail") String recipientEmail,
            @RequestParam("subject") String subject,
            @RequestParam("content") String content,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Optional<User> senderOptional = getCurrentUser(session);
        if (senderOptional.isEmpty()) {
            return "redirect:/login";
        }
        User sender = senderOptional.get();

        try {
            // Delegate all validation and sending logic to the service
            messageService.sendMessage(sender, recipientEmail, subject, content);

            redirectAttributes.addFlashAttribute("success", "Message sent successfully!");
            return "redirect:/inbox";
        } catch (IllegalArgumentException e) {
            // Catch validation errors (empty content, self-send) from the service
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/compose";
        } catch (RuntimeException e) {
            // Catch recipient not found or other runtime issues
            redirectAttributes.addFlashAttribute("error", "Error sending message: " + e.getMessage());
            return "redirect:/compose";
        }
    }

    @PostMapping("/send-reply")
    public String sendReply(
            @RequestParam("originalMessageId") Long originalMessageId,
            @RequestParam("content") String content,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Optional<User> senderOptional = getCurrentUser(session);
        if (senderOptional.isEmpty()) {
            return "redirect:/login";
        }
        User sender = senderOptional.get();

        // Prepare redirect URL for error handling
        String errorRedirect = "redirect:/read?messageId=" + originalMessageId;

        try {
            // Delegate all reply logic to the service
            messageService.sendReply(originalMessageId, content, sender);

            redirectAttributes.addFlashAttribute("success", "Reply sent successfully!");
            return "redirect:/inbox";
        } catch (IllegalArgumentException e) {
            // Catch validation errors (empty content, self-send) from the service
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return errorRedirect;
        } catch (RuntimeException e) {
            // Catch original message not found or other runtime issues
            redirectAttributes.addFlashAttribute("error", "Failed to send reply: " + e.getMessage());
            return errorRedirect;
        }
    }
}