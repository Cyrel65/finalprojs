package org.example.finalprojs.repository;

import org.example.finalprojs.model.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {

    // All notifications for a recipient, newest first
    List<AppNotification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);

    // Count unread notifications for a recipient
    long countByRecipientIdAndIsReadFalse(String recipientId);
}