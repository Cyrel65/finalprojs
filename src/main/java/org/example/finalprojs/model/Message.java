package org.example.finalprojs.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FIX: Change from @ManyToOne User entity to a raw String column (sender_id)
    // This allows the field to hold Teacher IDs, Student IDs, or external emails.
    @Column(name = "sender_id", nullable = false)
    private String senderId;

    // FIX: Change from @ManyToOne User entity to a raw String column (recipient_id)
    @Column(name = "recipient_id", nullable = false)
    private String recipientId;

    @Column(name = "subject", nullable = true)
    private String subject;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "is_read")
    private Boolean isRead = false;

    // Default constructor
    public Message() {}

    // Updated Constructor for creating a new message, now accepting String IDs
    public Message(String senderId, String recipientId, String subject, String content, LocalDateTime timestamp) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.subject = subject;
        this.content = content;
        this.timestamp = timestamp;
        this.isRead = false;
    }

    // --- Getters and Setters (Updated for String IDs) ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // NEW GETTER/SETTER for senderId (String)
    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    // NEW GETTER/SETTER for recipientId (String)
    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getRead() {
        return isRead;
    }

    public void setRead(Boolean read) {
        isRead = read;
    }

    // Getter (usually for boolean fields, it starts with 'is')
    public boolean isRead() {
        return isRead;
    }

    // Setter
    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }
}