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

    // --- REPLACED: Link to the new RedeemItem instead of the deprecated Box ---
    @ManyToOne
    @JoinColumn(name = "redeem_item_id")
    private RedeemItem redeemItem; // The specific redeemable reward item that was used

    private LocalDateTime redeemDate = LocalDateTime.now();

    // Constructors
    public RedeemTransaction() {
    }

    // Getters and Setters (Updated for RedeemItem)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    // --- Updated Getter/Setter ---
    public RedeemItem getRedeemItem() { return redeemItem; }
    public void setRedeemItem(RedeemItem redeemItem) { this.redeemItem = redeemItem; }
    // ---------------------------

    public LocalDateTime getRedeemDate() { return redeemDate; }
    public void setRedeemDate(LocalDateTime redeemDate) { this.redeemDate = redeemDate; }
}