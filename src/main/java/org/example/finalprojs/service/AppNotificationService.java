package org.example.finalprojs.service;

import org.example.finalprojs.model.AppNotification;
import org.example.finalprojs.repository.AppNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AppNotificationService {

    private final AppNotificationRepository notificationRepository;

    @Autowired
    public AppNotificationService(AppNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // Create and save a new notification
    public AppNotification createNotification(String recipientId, String name, String message) {
        AppNotification notification = new AppNotification(recipientId, name, message);
        return notificationRepository.save(notification);
    }

    // Get all notifications for a recipient
    public List<AppNotification> getNotifications(String recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    // Count unread notifications
    public long getUnreadCount(String recipientId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    // Mark one notification as read
    @Transactional
    public boolean markAsRead(Long id) {
        Optional<AppNotification> opt = notificationRepository.findById(id);
        if (opt.isPresent()) {
            AppNotification n = opt.get();
            n.setRead(true);
            notificationRepository.save(n);
            return true;
        }
        return false;
    }

    // Mark all notifications for a recipient as read
    @Transactional
    public void markAllAsRead(String recipientId) {
        List<AppNotification> notifications =
                notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
        for (AppNotification n : notifications) {
            n.setRead(true);
        }
        notificationRepository.saveAll(notifications);
    }

    // Delete a single notification
    @Transactional
    public boolean deleteNotification(Long id) {
        if (notificationRepository.existsById(id)) {
            notificationRepository.deleteById(id);
            return true;
        }
        return false;
    }
}