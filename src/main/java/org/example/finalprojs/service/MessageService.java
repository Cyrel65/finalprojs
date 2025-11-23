package org.example.finalprojs.service;

import org.example.finalprojs.model.Message;
import org.example.finalprojs.model.User;
import org.example.finalprojs.model.Teacher;
import org.example.finalprojs.repository.MessageRepository;
import org.example.finalprojs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final TeacherService teacherService;

    @Autowired
    public MessageService(MessageRepository messageRepository, UserRepository userRepository, TeacherService teacherService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.teacherService = teacherService;
    }

    // ⭐ FINAL CRITICAL FIX: Helper uses unique prefixes (T_ or S_) ⭐
    private String extractIdFromSenderObject(Object senderObject) {
        if (senderObject instanceof Teacher teacher) {
            // This must return "T_" + ID
            return "T_" + String.valueOf(teacher.getId());
        } else if (senderObject instanceof User user) {
            // This must return "S_" + ID
            return "S_" + String.valueOf(user.getId());
        } else {
            throw new IllegalArgumentException("Invalid sender/recipient object type passed to service.");
        }
    }

    // --- Display Name Lookup (Updated to handle T_ and S_ prefixes) ---
    public String getDisplayNameForId(String id) {
        if (id == null || id.isEmpty()) {
            return "Unknown User";
        }

        // 1. Check if ID is a raw email (external recipient/sender)
        if (id.contains("@")) {
            return id;
        }

        try {
            if (id.startsWith("T_")) {
                // Look up Teacher by ID without prefix
                String rawId = id.substring(2);
                Optional<Teacher> teacherOptional = teacherService.findTeacherById(rawId);
                if (teacherOptional.isPresent()) {
                    Teacher teacher = teacherOptional.get();
                    return teacher.getName() + " <" + teacher.getEmail() + ">";
                }
            } else if (id.startsWith("S_")) {
                // Look up Student by ID without prefix
                String rawId = id.substring(2);
                // Ensure the underlying ID is parsed as Long for the UserRepository if it uses a Long primary key
                Long longId = Long.valueOf(rawId);
                Optional<User> userOptional = userRepository.findById(longId);
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    return user.getName() + " <" + user.getEmail() + ">";
                }
            }
        } catch (NumberFormatException e) {
            // Ignore non-numeric ID attempts
        }

        return "ID: " + id + " (Not Found)";
    }

    // --- Message Retrieval (FIXED: Accepts Object) ---
    public List<Message> getInboxMessages(Object recipientObject) {
        String recipientId = extractIdFromSenderObject(recipientObject);
        return messageRepository.findByRecipientIdOrderByTimestampDesc(recipientId);
    }

    public List<Message> getSentMessages(Object senderObject) {
        String senderId = extractIdFromSenderObject(senderObject);
        return messageRepository.findBySenderIdOrderByTimestampDesc(senderId);
    }

    public Optional<Message> getMessageById(Long messageId) {
        return messageRepository.findById(messageId);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // --- Counters (FIXED: Accepts Object) ---
    public long getUnreadCount(Object recipientObject) {
        String recipientId = extractIdFromSenderObject(recipientObject);
        List<Message> receivedMessages = messageRepository.findByRecipientId(recipientId);
        return receivedMessages.stream().filter(m -> !m.isRead()).count();
    }

    public long getSentCount(Object senderObject) {
        String senderId = extractIdFromSenderObject(senderObject);
        return messageRepository.countBySenderId(senderId);
    }

    // --- Actions (FIXED: Accepts Object) ---
    @Transactional
    public void markMessageAsRead(Message message, Object currentUserObject) {
        String currentUserId = extractIdFromSenderObject(currentUserObject);
        if (message.getRecipientId().equals(currentUserId) && !message.isRead()) {
            message.setRead(true);
            messageRepository.save(message);
        }
    }

    @Transactional
    public Message sendMessage(Object senderObject, String recipientEmail, String subject, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Please select a recipient.");
        }

        String finalRecipientId;
        String senderId = extractIdFromSenderObject(senderObject); // Get correct sender ID (T_ID or S_ID)

        // Extract sender's email for self-send check
        String senderEmail;
        if (senderObject instanceof Teacher teacher) {
            senderEmail = teacher.getEmail();
        } else if (senderObject instanceof User user) {
            senderEmail = user.getEmail();
        } else {
            throw new IllegalArgumentException("Invalid sender object type passed.");
        }


        // 1. Try finding Recipient as Teacher
        Optional<Teacher> teacherRecipient = teacherService.findTeacherByEmail(recipientEmail);
        if (teacherRecipient.isPresent()) {
            // ⭐ Apply prefix here! ⭐
            finalRecipientId = "T_" + String.valueOf(teacherRecipient.get().getId());
        } else {
            // 2. Try finding Recipient as Student (User)
            Optional<User> userRecipient = userRepository.findByEmail(recipientEmail);
            if (userRecipient.isPresent()) {
                // ⭐ Apply prefix here! ⭐
                finalRecipientId = "S_" + String.valueOf(userRecipient.get().getId());
            }
            // 3. Treat as External Recipient
            else if (recipientEmail.contains("@")) {
                finalRecipientId = recipientEmail;
            } else {
                throw new RuntimeException("Recipient not found with email: " + recipientEmail);
            }
        }

        // Check for self-send using the internal ID/Email
        if (senderEmail.equalsIgnoreCase(recipientEmail)) {
            throw new IllegalArgumentException("Cannot send message to yourself.");
        }

        // Create message using String IDs
        Message message = new Message(
                senderId,                       // Corrected Sender ID (T_ID or S_ID)
                finalRecipientId,               // Recipient ID (T_ID or S_ID or Email)
                subject,
                content,
                LocalDateTime.now()
        );
        return messageRepository.save(message);
    }

    @Transactional
    public Message sendReply(Long originalMessageId, String content, Object senderObject) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Reply content cannot be empty.");
        }

        Message originalMessage = messageRepository.findById(originalMessageId)
                .orElseThrow(() -> new RuntimeException("Original message not found."));

        String replyRecipientId = originalMessage.getSenderId();
        String senderId = extractIdFromSenderObject(senderObject); // Get correct sender ID (T_ID or S_ID)

        if (senderId.equals(replyRecipientId)) {
            throw new IllegalArgumentException("Cannot send a reply to yourself.");
        }

        String replySubject = originalMessage.getSubject();
        if (!replySubject.toLowerCase().startsWith("re:")) {
            replySubject = "Re: " + originalMessage.getSubject();
        }

        // Create message using String IDs
        Message replyMessage = new Message(
                senderId,                       // Corrected Sender ID (T_ID or S_ID)
                replyRecipientId,               // Recipient ID is the original sender's ID (which has the prefix)
                replySubject,
                content,
                LocalDateTime.now()
        );
        return messageRepository.save(replyMessage);
    }

    // --- Authentication/Security Helpers (for Controller lookup) ---
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<Teacher> findTeacherByEmail(String email) {
        return teacherService.findTeacherByEmail(email);
    }

    public boolean userCanAccessMessage(Object userObject, Message message) {
        String currentUserId = extractIdFromSenderObject(userObject);
        // Check recipientId and senderId (both are now guaranteed to have prefixes or be an email)
        return message.getRecipientId().equals(currentUserId) ||
                message.getSenderId().equals(currentUserId);
    }
}