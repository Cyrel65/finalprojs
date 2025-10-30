// src/main/java/org/example/finalprojs/model/RedeemTransaction.java

package org.example.finalprojs.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class RedeemTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // The user who redeemed the item

    @ManyToOne
    @JoinColumn(name = "box_id")
    private Box box; // The box that was redeemed

    private LocalDateTime redeemDate = LocalDateTime.now();

    // Constructors
    public RedeemTransaction() {
    }

    // Getters and Setters (REQUIRED)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Box getBox() { return box; }
    public void setBox(Box box) { this.box = box; }

    public LocalDateTime getRedeemDate() { return redeemDate; }
    public void setRedeemDate(LocalDateTime redeemDate) { this.redeemDate = redeemDate; }
}