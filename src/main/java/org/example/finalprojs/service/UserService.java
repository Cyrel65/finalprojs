package org.example.finalprojs.service;

import org.example.finalprojs.model.User;
import org.example.finalprojs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public void registerUser(User user) {
        // Here you could add checks for email existence before saving
        userRepository.save(user);
    }

    /**
     * Authenticates a user based on email and password.
     * @return Optional<User> if successful, Optional.empty() otherwise.
     */
    public Optional<User> authenticate(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent() && userOptional.get().getPassword().equals(password)) {
            return userOptional;
        }
        return Optional.empty();
    }

    @Transactional
    public void updateProfilePicture(User user, String profilePictureUrl) {
        user.setProfilePictureUrl(profilePictureUrl);
        userRepository.save(user);
    }

    /**
     * Handles all profile update and validation logic (password check, email uniqueness).
     * Throws IllegalArgumentException on validation failure.
     */
    @Transactional
    public User updateProfile(User user, String name, String email, String currentPassword, String newPassword, String retypePassword) {
        // --- Verify current password ---
        if (!user.getPassword().equals(currentPassword)) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        // --- Check email uniqueness ---
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent() && !existing.get().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Email is already in use.");
        }

        // --- Update name and email ---
        user.setName(name);
        user.setEmail(email);

        // --- Update password if provided ---
        if (newPassword != null && !newPassword.isEmpty()) {
            if (!newPassword.equals(retypePassword)) {
                throw new IllegalArgumentException("New passwords do not match.");
            }
            user.setPassword(newPassword);
        }

        // --- Save changes ---
        return userRepository.save(user);
    }
}