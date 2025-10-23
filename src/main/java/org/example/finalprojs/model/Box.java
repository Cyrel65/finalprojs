package org.example.finalprojs.model;

import jakarta.persistence.*;

@Entity
@Table(name = "boxes")
public class Box {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int points; // For pointsInputInline
    private String typeOfTest; // For messageInputInline

    // --- Relationship to User (Foreign Key) ---
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Links this box to a specific user

    // Default Constructor (required by JPA)
    public Box() {}

    // Constructor for saving new boxes
    public Box(int points, String typeOfTest, User user) {
        this.points = points;
        this.typeOfTest = typeOfTest;
        this.user = user;
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getTypeOfTest() {
        return typeOfTest;
    }

    public void setTypeOfTest(String typeOfTest) {
        this.typeOfTest = typeOfTest;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}