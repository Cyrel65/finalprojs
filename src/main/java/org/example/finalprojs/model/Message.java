package org.example.finalprojs.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Connects to the User entity for the sender
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // Connects to the User entity for the recipient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    // NEW FIELD: Subject
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

    // Updated Constructor for creating a new message
    public Message(User sender, User recipient, String subject, String content, LocalDateTime timestamp) {
        this.sender = sender;
        this.recipient = recipient;
        this.subject = subject; // Set the subject
        this.content = content;
        this.timestamp = timestamp;
        this.isRead = false;
    }

    // --- Getters and Setters (REQUIRED FOR JPA/THYMELEAF) ---
    // Please ensure all necessary getters and setters are present in your full file,
    // including the new ones for 'subject'.

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
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