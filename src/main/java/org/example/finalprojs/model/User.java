package org.example.finalprojs.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users") // Maps this class to a table named 'users' in MySQL
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // These field names must match the th:field attributes in your HTML
    private String name;
    private String email;
    private String password; // 🚨 IMPORTANT: In production, this MUST be hashed!

    // Default Constructor (required by JPA)
    public User() {
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}