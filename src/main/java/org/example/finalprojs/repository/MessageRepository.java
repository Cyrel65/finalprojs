package org.example.finalprojs.repository;

import org.example.finalprojs.model.Message;
import org.example.finalprojs.model.User; // Import User
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Finds all messages where the given User is the recipient, ordered by newest first.
     * This is the method needed to populate the inbox.
     */
    List<Message> findByRecipientOrderByTimestampDesc(User recipient);




    @Query("SELECT m FROM Message m " +
            "JOIN FETCH m.sender " +
            "JOIN FETCH m.recipient " +
            "WHERE m.sender.id = :userId OR m.recipient.id = :userId " +
            "ORDER BY m.timestamp DESC")
    List<Message> findAllMessagesWithUsers(@Param("userId") Long userId);

    @Query("SELECT m FROM Message m " +
            "JOIN FETCH m.sender " +
            "JOIN FETCH m.recipient " +
            "WHERE (m.sender.id = :user1Id AND m.recipient.id = :user2Id) " +
            "   OR (m.sender.id = :user2Id AND m.recipient.id = :user1Id) " +
            "ORDER BY m.timestamp ASC")
    List<Message> findConversationBetweenUsers(@Param("user1Id") Long user1Id,
                                               @Param("user2Id") Long user2Id);

    // 2. Fetch sent messages ordered by time (REQUIRED FIX for /sent list)
    List<Message> findBySenderOrderByTimestampDesc(User sender);

    // REQUIRED NEW METHOD for counting sent items:
    long countBySender(User sender);

    // For calculating the unread count (used in Inbox badge)
    List<Message> findByRecipient(User recipient);



}