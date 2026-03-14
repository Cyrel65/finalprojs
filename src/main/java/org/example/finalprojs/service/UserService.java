package org.example.finalprojs.service;

import org.example.finalprojs.model.User;
import org.example.finalprojs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final Map<String, PasswordResetToken> tokenStore = new HashMap<>();

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ADDED: This method allows the MainController to fetch Joseph and others by section
    public List<User> findBySection(String section) {
        return userRepository.findBySection(section);
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public void registerUser(User user) {
        userRepository.save(user);
    }

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

    @Transactional
    public User updateProfile(User user, String name, String email, String currentPassword, String newPassword, String retypePassword) {
        if (!user.getPassword().equals(currentPassword)) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent() && !existing.get().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Email is already in use.");
        }

        user.setName(name);
        user.setEmail(email);

        if (newPassword != null && !newPassword.isEmpty()) {
            if (!newPassword.equals(retypePassword)) {
                throw new IllegalArgumentException("New passwords do not match.");
            }
            user.setPassword(newPassword);
        }

        return userRepository.save(user);
    }

    public String createPasswordResetToken(User user) {
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, new PasswordResetToken(user, LocalDateTime.now().plusMinutes(30)));
        return token;
    }

    public Optional<User> validatePasswordResetToken(String token) {
        PasswordResetToken prt = tokenStore.get(token);
        if (prt == null || prt.getExpiry().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        return Optional.of(prt.getUser());
    }

    @Transactional
    public User updatePassword(String token, String newPassword) {
        Optional<User> userOptional = validatePasswordResetToken(token);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired token.");
        }
        User user = userOptional.get();
        user.setPassword(newPassword);
        userRepository.save(user);
        tokenStore.remove(token);
        return user;
    }

    private static class PasswordResetToken {
        private final User user;
        private final LocalDateTime expiry;

        public PasswordResetToken(User user, LocalDateTime expiry) {
            this.user = user;
            this.expiry = expiry;
        }

        public User getUser() { return user; }
        public LocalDateTime getExpiry() { return expiry; }
    }
}