package org.example.finalprojs.controller;

import jakarta.servlet.http.HttpSession;
import org.example.finalprojs.model.Message;
import org.example.finalprojs.model.User;
import org.example.finalprojs.model.Teacher;
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


    // 🛑 CRITICAL FIX: Helper method now correctly searches BOTH User (Student) and Teacher tables
    // to retrieve the specific object needed by the MessageService.
    private Object getCurrentSender(HttpSession session) {
        // Assume 'userEmail' is set upon successful login for both Teachers and Students
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null || userEmail.isEmpty()) {
            return null;
        }

        // 1. Try finding Teacher
        // NOTE: This relies on MessageService having a findTeacherByEmail method.
        Optional<Teacher> teacherOptional = messageService.findTeacherByEmail(userEmail);
        if (teacherOptional.isPresent()) {
            return teacherOptional.get();
        }

        // 2. Try finding Student (User)
        // NOTE: This relies on MessageService having a findUserByEmail method.
        Optional<User> userOptional = messageService.findUserByEmail(userEmail);
        if (userOptional.isPresent()) {
            return userOptional.get();
        }

        return null;
    }

    // --- Dashboard Handler ---

    @GetMapping("/")
    public String viewDashboard(Model model, HttpSession session) {
        Object senderObject = getCurrentSender(session);
        if (senderObject == null) {
            return "redirect:/login";
        }

        // Pass the generic object to the model and service methods
        model.addAttribute("user", senderObject);
        // FIX: Consolidate service calls to optimize
        model.addAttribute("unreadCount", messageService.getUnreadCount(senderObject));
        model.addAttribute("sentCount", messageService.getSentCount(senderObject));
        model.addAttribute("pageTitle", "Dashboard");

        return "index";
    }

    // --- Message/Email Handlers ---

    @GetMapping("/inbox")
    public String viewInbox(Model model, HttpSession session) {
        Object senderObject = getCurrentSender(session);
        if (senderObject == null) {
            return "redirect:/login";
        }

        // Delegate retrieval to service
        List<Message> receivedMessages = messageService.getInboxMessages(senderObject);

        // FIX: Pass the user object for template access (e.g., displaying name/email)
        model.addAttribute("user", senderObject);
        model.addAttribute("messageService", messageService);
        model.addAttribute("messages", receivedMessages);
        model.addAttribute("unreadCount", messageService.getUnreadCount(senderObject));
        model.addAttribute("sentCount", messageService.getSentCount(senderObject));
        model.addAttribute("pageTitle", "Inbox");

        return "email-inbox";
    }

    @GetMapping("/sent")
    public String sentMessages(Model model, HttpSession session) {
        Object senderObject = getCurrentSender(session);
        if (senderObject == null) {
            return "redirect:/login";
        }

        // Delegate retrieval to service
        List<Message> sentMessages = messageService.getSentMessages(senderObject);

        model.addAttribute("user", senderObject);
        model.addAttribute("messageService", messageService);
        model.addAttribute("messages", sentMessages);
        model.addAttribute("sentCount", messageService.getSentCount(senderObject));
        model.addAttribute("unreadCount", messageService.getUnreadCount(senderObject));
        model.addAttribute("pageTitle", "Sent Messages");

        return "email-inbox";
    }

    @GetMapping("/read")
    public String viewRead(@RequestParam Long messageId, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Object senderObject = getCurrentSender(session);
        if (senderObject == null) {
            return "redirect:/login";
        }

        Optional<Message> messageOptional = messageService.getMessageById(messageId);
        if (messageOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Message not found.");
            return "redirect:/inbox";
        }

        Message message = messageOptional.get();

        // Security Check: Delegate access validation to the service
        if (!messageService.userCanAccessMessage(senderObject, message)) {
            redirectAttributes.addFlashAttribute("error", "Access denied. This message is not yours.");
            return "redirect:/inbox";
        }

        // Mark as Read Logic: Delegate to the service
        messageService.markMessageAsRead(message, senderObject);

        // Display names are crucial here
        model.addAttribute("senderDisplayName", messageService.getDisplayNameForId(message.getSenderId()));
        model.addAttribute("recipientDisplayName", messageService.getDisplayNameForId(message.getRecipientId()));

        model.addAttribute("user", senderObject);
        model.addAttribute("message", message);
        model.addAttribute("unreadCount", messageService.getUnreadCount(senderObject));
        model.addAttribute("sentCount", messageService.getSentCount(senderObject));

        return "email-read";
    }

    @GetMapping("/compose")
    public String viewCompose(Model model, HttpSession session) {
        Object senderObject = getCurrentSender(session);
        if (senderObject == null) {
            return "redirect:/login";
        }

        // FIX: If you use the User list for a dropdown, ensure you handle Teachers too.
        // For now, only sending 'User' list as per original code, but if Teachers are needed,
        // messageService.getAllUsers() must be updated to combine both User and Teacher lists.
        List<User> allUsers = messageService.getAllUsers();

        model.addAttribute("user", senderObject);
        model.addAttribute("unreadCount", messageService.getUnreadCount(senderObject));
        model.addAttribute("sentCount", messageService.getSentCount(senderObject));
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

        Object senderObject = getCurrentSender(session);
        if (senderObject == null) {
            return "redirect:/login";
        }

        try {
            // Pass the generic sender object to the service
            messageService.sendMessage(senderObject, recipientEmail, subject, content);

            redirectAttributes.addFlashAttribute("success", "Message sent successfully!");
            return "redirect:/inbox";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/compose";
        } catch (RuntimeException e) {
            // Catch broader errors like Recipient not found, database errors, etc.
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

        Object senderObject = getCurrentSender(session);
        if (senderObject == null) {
            return "redirect:/login";
        }

        // Prepare redirect URL for error handling
        String errorRedirect = "redirect:/read?messageId=" + originalMessageId;

        try {
            // Pass the generic sender object to the service
            messageService.sendReply(originalMessageId, content, senderObject);

            redirectAttributes.addFlashAttribute("success", "Reply sent successfully!");
            return "redirect:/inbox";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return errorRedirect;
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to send reply: " + e.getMessage());
            return errorRedirect;
        }
    }
}