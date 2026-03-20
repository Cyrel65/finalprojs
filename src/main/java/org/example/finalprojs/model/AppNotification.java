package org.example.finalprojs.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "app_notifications")
public class AppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who receives this notification — stored as "T_1" or "S_1"
    @Column(nullable = false)
    private String recipientId;

    // Short title e.g. "New message from Joseph"
    @Column(nullable = false)
    private String name;

    // Body text e.g. "Hey, can I ask about the exam?"
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean isRead = false;

    public AppNotification() {}

    public AppNotification(String recipientId, String name, String message) {
        this.recipientId = recipientId;
        this.name        = name;
        this.message     = message;
        this.createdAt   = LocalDateTime.now();
        this.isRead      = false;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }

    public String getRecipientId()              { return recipientId; }
    public void setRecipientId(String r)        { this.recipientId = r; }

    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }

    public String getMessage()                  { return message; }
    public void setMessage(String message)      { this.message = message; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime t)   { this.createdAt = t; }

    public boolean isRead()                     { return isRead; }
    public void setRead(boolean read)           { this.isRead = read; }

    // Helper: formatted date string for Flutter display e.g. "2026-03-20"
    public String getDate() {
        return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    // Helper: formatted time string for Flutter display e.g. "14:30"
    public String getTime() {
        return createdAt.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}