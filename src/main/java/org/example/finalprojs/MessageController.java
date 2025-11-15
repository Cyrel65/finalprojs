package org.example.finalprojs;

import jakarta.servlet.http.HttpSession;
import org.example.finalprojs.model.Message;
import org.example.finalprojs.model.User;
import org.example.finalprojs.repository.MessageRepository;
import org.example.finalprojs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class MessageController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;


    // Private Helper Method for Session User
    private Optional<User> getCurrentUser(HttpSession session) {
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(userEmail);
    }

    // --- Message/Email Handlers ---

    // Inside MessageController.java

    @GetMapping("/inbox")
    public String viewInbox(Model model, HttpSession session) {
        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = userOptional.get();

        // Fix 1: Add currentUser for the header/sidebar profile picture
        model.addAttribute("user", currentUser);

        // 1. Fetch messages for the inbox, sorted by timestamp
        List<Message> receivedMessages = messageRepository.findByRecipientOrderByTimestampDesc(currentUser);
        model.addAttribute("messages", receivedMessages);

        // 2. Calculate UNREAD count (already here, but ensuring it's in the model)
        long unreadCount = receivedMessages.stream().filter(m -> !m.isRead()).count();
        model.addAttribute("unreadCount", unreadCount);

        // Fix 2: Calculate SENT count for the header/sidebar badge
        long sentCount = messageRepository.countBySender(currentUser);
        model.addAttribute("sentCount", sentCount);

        // Set active menu item title
        model.addAttribute("pageTitle", "Inbox");

        return "email-inbox";
    }

    @GetMapping("/read")
    public String viewRead(@RequestParam Long messageId, Model model, HttpSession session) {
        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = userOptional.get();

        // Fix 1: Add currentUser for the header/sidebar profile picture
        model.addAttribute("user", currentUser);

        Optional<Message> messageOptional = messageRepository.findById(messageId);
        if (messageOptional.isEmpty()) {
            model.addAttribute("error", "Message not found.");
            return "email-inbox"; // Redirect to inbox if not found
        }

        Message message = messageOptional.get();

        // --- Security Check: (No change) ---
        Long currentUserId = currentUser.getId();

        if (!message.getRecipient().getId().equals(currentUserId) &&
                !message.getSender().getId().equals(currentUserId)) {

            model.addAttribute("error", "Access denied. This message is neither in your inbox nor your sent items.");
            return "email-inbox";
        }

        // --- Mark as Read Logic: (No change) ---
        if (message.getRecipient().getId().equals(currentUserId) && !message.isRead()) {
            message.setRead(true);
            messageRepository.save(message);
        }

        model.addAttribute("message", message);

        // Fix 2: Add Message Counts for the header/sidebar badges
        List<Message> allReceivedMessages = messageRepository.findByRecipient(currentUser);
        long unreadCount = allReceivedMessages.stream().filter(m -> !m.isRead()).count();
        model.addAttribute("unreadCount", unreadCount);

        long sentCount = messageRepository.countBySender(currentUser);
        model.addAttribute("sentCount", sentCount);

        return "email-read";
    }

    @GetMapping("/compose")
    public String viewCompose(Model model, HttpSession session) {
        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = userOptional.get();

        // Fix 1: Add currentUser for the header/sidebar profile picture
        model.addAttribute("user", currentUser);

        // Fix 2: Add Message Counts for the header/sidebar badges
        List<Message> receivedMessages = messageRepository.findByRecipient(currentUser);
        long unreadCount = receivedMessages.stream().filter(m -> !m.isRead()).count();
        model.addAttribute("unreadCount", unreadCount);

        long sentCount = messageRepository.countBySender(currentUser);
        model.addAttribute("sentCount", sentCount);

        // Populate model with all users so the user can select a recipient (Your original logic)
        List<User> allUsers = userRepository.findAll();
        model.addAttribute("allUsers", allUsers);

        // Set page title
        model.addAttribute("pageTitle", "Compose Message");

        return "email-compose";
    }

    @PostMapping("/send-message")
    public String sendMessage(
            @RequestParam("recipientEmail") String recipientEmail,
            @RequestParam("subject") String subject, // Added subject field
            @RequestParam("content") String content,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Optional<User> senderOptional = getCurrentUser(session);
        if (senderOptional.isEmpty()) {
            return "redirect:/login";
        }
        User sender = senderOptional.get();

        if (content == null || content.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Message content cannot be empty.");
            return "redirect:/compose";
        }

        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a recipient.");
            return "redirect:/compose";
        }

        try {
            User recipient = userRepository.findByEmail(recipientEmail)
                    .orElseThrow(() -> new RuntimeException("Recipient not found with email: " + recipientEmail));

            if (sender.getEmail().equalsIgnoreCase(recipientEmail)) {
                redirectAttributes.addFlashAttribute("error", "Cannot send message to yourself.");
                return "redirect:/compose";
            }

            Message message = new Message(
                    sender,
                    recipient,
                    subject, // Assuming your Message model has a subject field
                    content,
                    LocalDateTime.now()
            );
            messageRepository.save(message);

            redirectAttributes.addFlashAttribute("success", "Message sent successfully!");
            return "redirect:/inbox";
        } catch (RuntimeException e) {
            // Use the generic 'error' flash attribute for better display consistency
            redirectAttributes.addFlashAttribute("error", "Error sending message: " + e.getMessage());
            return "redirect:/compose";
        }
    }

    // Inside your MessageController.java

    @PostMapping("/send-reply")
    public String sendReply(
            @RequestParam("originalMessageId") Long originalMessageId,
            @RequestParam("content") String content,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // 1. Authenticate the current user (the replier)
        Optional<User> senderOptional = getCurrentUser(session);
        if (senderOptional.isEmpty()) {
            // If no user is logged in, redirect to login
            return "redirect:/login";
        }
        User sender = senderOptional.get();

        if (content == null || content.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Reply content cannot be empty.");
            // Redirect back to the original message view
            return "redirect:/read/" + originalMessageId;
        }

        try {
            // 2. Find the original message to get the recipient (the original sender)
            Message originalMessage = messageRepository.findById(originalMessageId)
                    .orElseThrow(() -> new RuntimeException("Original message not found."));

            // 3. Determine the recipient (The recipient of the reply is the sender of the original message)
            User recipient = originalMessage.getSender();

            // Optional: Prevent sending a reply if the original message sender is also the current user
            if (sender.getId().equals(recipient.getId())) {
                redirectAttributes.addFlashAttribute("error", "Cannot send a reply to yourself.");
                return "redirect:/read/" + originalMessageId;
            }

            // 4. Construct the subject for the reply
            String replySubject = "Re: " + originalMessage.getSubject();
            if (!replySubject.startsWith("Re: ")) {
                replySubject = "Re: " + originalMessage.getSubject();
            }

            // 5. Create and save the new reply message
            Message replyMessage = new Message(
                    sender,
                    recipient,
                    replySubject,
                    content,
                    LocalDateTime.now()
            );
            messageRepository.save(replyMessage);

            redirectAttributes.addFlashAttribute("success", "Reply sent successfully!");
            return "redirect:/inbox"; // Redirect to inbox after successful send
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to send reply: " + e.getMessage());
            return "redirect:/read/" + originalMessageId; // Redirect back to the view page on error
        }
    }

    // Inside MessageController.java

    // Inside MessageController.java

    @GetMapping("/")
    public String viewDashboard(Model model, HttpSession session) {
        // 1. Authentication Check
        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = userOptional.get();

        // 2. Add the User object (Fixes the SpEL error)
        model.addAttribute("user", currentUser);

        // 3. Add Message Counts (required by the header/sidebar)
        List<Message> receivedMessages = messageRepository.findByRecipient(currentUser);
        long unreadCount = receivedMessages.stream().filter(m -> !m.isRead()).count();
        model.addAttribute("unreadCount", unreadCount);

        long sentCount = messageRepository.countBySender(currentUser);
        model.addAttribute("sentCount", sentCount);

        // 4. Set page title
        model.addAttribute("pageTitle", "Dashboard");

        return "index";
    }

    @GetMapping("/sent")
    public String sentMessages(Model model, HttpSession session) {
        Optional<User> currentUserOptional = getCurrentUser(session);
        if (currentUserOptional.isEmpty()) {
            // 1. If no user is logged in, redirect to login
            return "redirect:/login";
        }
        User currentUser = currentUserOptional.get();

        // 2. Fetch messages where the current user is the SENDER
        List<Message> sentMessages = messageRepository.findBySenderOrderByTimestampDesc(currentUser);
        model.addAttribute("messages", sentMessages);

        // 3. Calculate SENT count
        long sentCount = messageRepository.countBySender(currentUser);
        model.addAttribute("sentCount", sentCount);

        // 4. Calculate UNREAD count
        List<Message> receivedMessages = messageRepository.findByRecipient(currentUser);
        long unreadCount = receivedMessages.stream().filter(m -> !m.isRead()).count();
        model.addAttribute("unreadCount", unreadCount);

        // 5. CRITICAL FIX: Add the currentUser object to the model
        // This allows the header (profile picture) to render without error.
        model.addAttribute("user", currentUser);


        // 6. Add a title variable to highlight the active menu item
        model.addAttribute("pageTitle", "Sent Messages");

        // 7. Return the template
        return "email-inbox";
    }
}