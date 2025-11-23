package org.example.finalprojs.repository;

import org.example.finalprojs.model.Message;
import org.example.finalprojs.model.User; // Keep Import User if needed elsewhere, but not for these methods
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Finds all messages where the given ID (String) is the recipient, ordered by newest first.
     * FIX: Changed parameter from User to String ID.
     */
    List<Message> findByRecipientIdOrderByTimestampDesc(String recipientId);

    // --- FIX: The Custom Queries are likely no longer needed/won't work after model change ---
    // If you need these, they must be completely rewritten to use String senderId/recipientId
    // and cannot use JOIN FETCH on User entities as before.

    // @Query("SELECT m FROM Message m " + ...
    // List<Message> findAllMessagesWithUsers(@Param("userId") Long userId);

    // @Query("SELECT m FROM Message m " + ...
    // List<Message> findConversationBetweenUsers(@Param("user1Id") Long user1Id, ...);


    /**
     * Fetch sent messages ordered by time.
     * FIX: Changed parameter from User to String ID.
     */
    List<Message> findBySenderIdOrderByTimestampDesc(String senderId);

    /**
     * REQUIRED NEW METHOD for counting sent items:
     * FIX: Changed parameter from User to String ID.
     */
    long countBySenderId(String senderId);

    /**
     * For calculating the unread count (used in Inbox badge).
     * FIX: Changed parameter from User to String ID.
     */
    List<Message> findByRecipientId(String recipientId);
}