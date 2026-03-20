package org.example.finalprojs.service;

import org.example.finalprojs.model.AppNotification;
import org.example.finalprojs.model.Message;
import org.example.finalprojs.model.User;
import org.example.finalprojs.model.Teacher;
import org.example.finalprojs.repository.AppNotificationRepository;
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
    private final AppNotificationRepository notificationRepository;  // NEW

    @Autowired
    public MessageService(MessageRepository messageRepository,
                          UserRepository userRepository,
                          TeacherService teacherService,
                          AppNotificationRepository notificationRepository) {
        this.messageRepository    = messageRepository;
        this.userRepository       = userRepository;
        this.teacherService       = teacherService;
        this.notificationRepository = notificationRepository;
    }

    // ── ID helpers ────────────────────────────────────────────────────────────

    private String extractIdFromSenderObject(Object senderObject) {
        if (senderObject instanceof Teacher teacher) {
            return "T_" + teacher.getId();
        } else if (senderObject instanceof User user) {
            return "S_" + user.getId();
        } else {
            throw new IllegalArgumentException("Invalid sender/recipient object type.");
        }
    }

    public String getDisplayNameForId(String id) {
        if (id == null || id.isEmpty()) return "Unknown User";
        if (id.contains("@")) return id;

        try {
            if (id.startsWith("T_")) {
                String rawId = id.substring(2);
                Optional<Teacher> t = teacherService.findTeacherById(rawId);
                if (t.isPresent()) return t.get().getName() + " <" + t.get().getEmail() + ">";
            } else if (id.startsWith("S_")) {
                Long longId = Long.valueOf(id.substring(2));
                Optional<User> u = userRepository.findById(longId);
                if (u.isPresent()) return u.get().getName() + " <" + u.get().getEmail() + ">";
            }
        } catch (NumberFormatException ignored) {}

        return "ID: " + id + " (Not Found)";
    }

    // ── NEW: Get sender's display name from prefixed ID ───────────────────────

    private String getSenderName(String prefixedId) {
        try {
            if (prefixedId.startsWith("T_")) {
                String rawId = prefixedId.substring(2);
                return teacherService.findTeacherById(rawId)
                        .map(Teacher::getName).orElse("Teacher");
            } else if (prefixedId.startsWith("S_")) {
                Long id = Long.valueOf(prefixedId.substring(2));
                return userRepository.findById(id)
                        .map(User::getName).orElse("Student");
            }
        } catch (Exception ignored) {}
        return "User";
    }

    // ── NEW: Create a notification for the recipient ──────────────────────────
    // Called after every message save so the teacher gets notified
    // whenever a student sends a message from the website.

    private void createNotificationForRecipient(String recipientId,
                                                String senderName,
                                                String messageContent) {
        try {
            String preview = messageContent != null && messageContent.length() > 60
                    ? messageContent.substring(0, 60) + "…"
                    : messageContent;

            AppNotification notification = new AppNotification(
                    recipientId,
                    "New message from " + senderName,
                    preview != null ? preview : ""
            );
            notificationRepository.save(notification);
        } catch (Exception e) {
            // Don't let notification failure break message sending
            System.err.println("Failed to create notification: " + e.getMessage());
        }
    }

    // ── Message retrieval ─────────────────────────────────────────────────────

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

    // ── Counters ──────────────────────────────────────────────────────────────

    public long getUnreadCount(Object recipientObject) {
        String recipientId = extractIdFromSenderObject(recipientObject);
        List<Message> receivedMessages = messageRepository.findByRecipientId(recipientId);
        return receivedMessages.stream().filter(m -> !m.isRead()).count();
    }

    public long getSentCount(Object senderObject) {
        String senderId = extractIdFromSenderObject(senderObject);
        return messageRepository.countBySenderId(senderId);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    @Transactional
    public void markMessageAsRead(Message message, Object currentUserObject) {
        String currentUserId = extractIdFromSenderObject(currentUserObject);
        if (message.getRecipientId().equals(currentUserId) && !message.isRead()) {
            message.setRead(true);
            messageRepository.save(message);
        }
    }

    // UPDATED: now creates a notification for the recipient after saving
    @Transactional
    public Message sendMessage(Object senderObject, String recipientEmail,
                               String subject, String content) {
        if (content == null || content.trim().isEmpty())
            throw new IllegalArgumentException("Message content cannot be empty.");
        if (recipientEmail == null || recipientEmail.trim().isEmpty())
            throw new IllegalArgumentException("Please select a recipient.");

        String senderId = extractIdFromSenderObject(senderObject);

        String senderEmail;
        if (senderObject instanceof Teacher t) senderEmail = t.getEmail();
        else if (senderObject instanceof User u) senderEmail = u.getEmail();
        else throw new IllegalArgumentException("Invalid sender object type.");

        // Resolve recipient
        String finalRecipientId;
        Optional<Teacher> teacherRecipient = teacherService.findTeacherByEmail(recipientEmail);
        if (teacherRecipient.isPresent()) {
            finalRecipientId = "T_" + teacherRecipient.get().getId();
        } else {
            Optional<User> userRecipient = userRepository.findByEmail(recipientEmail);
            if (userRecipient.isPresent()) {
                finalRecipientId = "S_" + userRecipient.get().getId();
            } else if (recipientEmail.contains("@")) {
                finalRecipientId = recipientEmail;
            } else {
                throw new RuntimeException("Recipient not found: " + recipientEmail);
            }
        }

        if (senderEmail.equalsIgnoreCase(recipientEmail))
            throw new IllegalArgumentException("Cannot send message to yourself.");

        Message message = new Message(senderId, finalRecipientId,
                subject, content, LocalDateTime.now());
        messageRepository.save(message);

        // ── NEW: notify the recipient ─────────────────────────────────────────
        String senderName = getSenderName(senderId);
        createNotificationForRecipient(finalRecipientId, senderName, content);

        return message;
    }

    // UPDATED: now creates a notification for the original sender after reply
    @Transactional
    public Message sendReply(Long originalMessageId, String content,
                             Object senderObject) {
        if (content == null || content.trim().isEmpty())
            throw new IllegalArgumentException("Reply content cannot be empty.");

        Message originalMessage = messageRepository.findById(originalMessageId)
                .orElseThrow(() -> new RuntimeException("Original message not found."));

        String replyRecipientId = originalMessage.getSenderId();
        String senderId         = extractIdFromSenderObject(senderObject);

        if (senderId.equals(replyRecipientId))
            throw new IllegalArgumentException("Cannot send a reply to yourself.");

        String replySubject = originalMessage.getSubject();
        if (!replySubject.toLowerCase().startsWith("re:"))
            replySubject = "Re: " + replySubject;

        Message replyMessage = new Message(senderId, replyRecipientId,
                replySubject, content, LocalDateTime.now());
        messageRepository.save(replyMessage);

        // ── NEW: notify the recipient of the reply ────────────────────────────
        String senderName = getSenderName(senderId);
        createNotificationForRecipient(replyRecipientId, senderName, content);

        return replyMessage;
    }

    // ── Auth helpers ──────────────────────────────────────────────────────────

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<Teacher> findTeacherByEmail(String email) {
        return teacherService.findTeacherByEmail(email);
    }

    public boolean userCanAccessMessage(Object userObject, Message message) {
        String currentUserId = extractIdFromSenderObject(userObject);
        return message.getRecipientId().equals(currentUserId) ||
                message.getSenderId().equals(currentUserId);
    }
}