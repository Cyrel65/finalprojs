package org.example.finalprojs.service;

import org.example.finalprojs.model.Message;
import org.example.finalprojs.model.User;
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

    @Autowired
    public MessageService(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    // --- Message Retrieval ---

    public List<Message> getInboxMessages(User recipient) {
        return messageRepository.findByRecipientOrderByTimestampDesc(recipient);
    }

    public List<Message> getSentMessages(User sender) {
        return messageRepository.findBySenderOrderByTimestampDesc(sender);
    }

    public Optional<Message> getMessageById(Long messageId) {
        return messageRepository.findById(messageId);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // --- Counters ---

    public long getUnreadCount(User recipient) {
        List<Message> receivedMessages = messageRepository.findByRecipient(recipient);
        return receivedMessages.stream().filter(m -> !m.isRead()).count();
    }

    public long getSentCount(User sender) {
        return messageRepository.countBySender(sender);
    }

    // --- Actions ---

    @Transactional
    public void markMessageAsRead(Message message, User currentUser) {
        // Only mark as read if the current user is the recipient and the message is unread
        if (message.getRecipient().getId().equals(currentUser.getId()) && !message.isRead()) {
            message.setRead(true);
            messageRepository.save(message);
        }
    }

    @Transactional
    public Message sendMessage(User sender, String recipientEmail, String subject, String content) {
        // Validation checks
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Please select a recipient.");
        }

        User recipient = userRepository.findByEmail(recipientEmail)
                .orElseThrow(() -> new RuntimeException("Recipient not found with email: " + recipientEmail));

        if (sender.getEmail().equalsIgnoreCase(recipientEmail)) {
            throw new IllegalArgumentException("Cannot send message to yourself.");
        }

        Message message = new Message(
                sender,
                recipient,
                subject,
                content,
                LocalDateTime.now()
        );
        return messageRepository.save(message);
    }

    @Transactional
    public Message sendReply(Long originalMessageId, String content, User sender) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Reply content cannot be empty.");
        }

        Message originalMessage = messageRepository.findById(originalMessageId)
                .orElseThrow(() -> new RuntimeException("Original message not found."));

        // Recipient of the reply is the sender of the original message
        User recipient = originalMessage.getSender();

        if (sender.getId().equals(recipient.getId())) {
            throw new IllegalArgumentException("Cannot send a reply to yourself.");
        }

        // Construct the subject for the reply
        String replySubject = originalMessage.getSubject();
        if (!replySubject.toLowerCase().startsWith("re:")) {
            replySubject = "Re: " + originalMessage.getSubject();
        }

        Message replyMessage = new Message(
                sender,
                recipient,
                replySubject,
                content,
                LocalDateTime.now()
        );
        return messageRepository.save(replyMessage);
    }

    // --- Authentication/Security Helpers ---

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean userCanAccessMessage(User user, Message message) {
        Long currentUserId = user.getId();
        return message.getRecipient().getId().equals(currentUserId) ||
                message.getSender().getId().equals(currentUserId);
    }
}