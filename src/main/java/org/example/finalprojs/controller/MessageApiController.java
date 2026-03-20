package org.example.finalprojs.controller;

import org.example.finalprojs.model.Message;
import org.example.finalprojs.model.Teacher;
import org.example.finalprojs.model.User;
import org.example.finalprojs.repository.MessageRepository;
import org.example.finalprojs.repository.TeacherRepository;
import org.example.finalprojs.repository.UserRepository;
import org.example.finalprojs.service.AppNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageApiController {

    @Autowired private MessageRepository messageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private AppNotificationService notificationService;

    // ── GET /api/messages/teacher/{teacherId} ─────────────────────────────────
    // Returns all messages where the teacher is sender OR recipient.
    // Flutter uses this to populate the inbox and sent tabs.

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<Map<String, Object>>> getTeacherMessages(
            @PathVariable String teacherId) {
        String tid = "T_" + teacherId;
        List<Message> messages = messageRepository.findAll().stream()
                .filter(m -> m.getSenderId().equals(tid) || m.getRecipientId().equals(tid))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(messages.stream().map(this::toMap).collect(Collectors.toList()));
    }

    // ── POST /api/messages/send ───────────────────────────────────────────────
    // Called by Flutter to send a message from teacher to student.
    // Body: { senderId, recipientEmail, content }
    // senderId is the raw teacher numeric ID (Flutter sends it without prefix).

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> payload) {
        try {
            String rawSenderId    = payload.get("senderId").toString();
            String recipientEmail = payload.get("recipientEmail") != null
                    ? payload.get("recipientEmail").toString()
                    : payload.getOrDefault("recipientId", "").toString();
            String content        = payload.get("content").toString();

            // Build prefixed sender ID
            String senderId = rawSenderId.startsWith("T_") || rawSenderId.startsWith("S_")
                    ? rawSenderId
                    : "T_" + rawSenderId;

            // Resolve recipient by email
            String finalRecipientId;
            String recipientName;

            Optional<User> studentOpt = userRepository.findByEmail(recipientEmail);
            if (studentOpt.isPresent()) {
                finalRecipientId = "S_" + studentOpt.get().getId();
                recipientName    = studentOpt.get().getName();
            } else {
                Optional<Teacher> teacherOpt = teacherRepository.findByEmail(recipientEmail);
                if (teacherOpt.isPresent()) {
                    finalRecipientId = "T_" + teacherOpt.get().getId();
                    recipientName    = teacherOpt.get().getName();
                } else {
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", "Recipient not found: " + recipientEmail));
                }
            }

            // Save message
            Message message = new Message(
                    senderId, finalRecipientId,
                    "Chat", content, LocalDateTime.now());
            messageRepository.save(message);

            // Get sender name for notification
            String senderName = getSenderName(senderId);

            // Create notification for the recipient
            notificationService.createNotification(
                    finalRecipientId,
                    "New message from " + senderName,
                    content.length() > 60 ? content.substring(0, 60) + "…" : content
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Sent successfully",
                    "recipientId", finalRecipientId
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to send: " + e.getMessage()));
        }
    }

    // ── POST /api/messages/send-to-id ─────────────────────────────────────────
    // Called by Flutter chat when replying — recipient is already known by ID.
    // Body: { senderId, recipientId, content }
    // Both IDs may or may not have T_/S_ prefix — we handle both.

    @PostMapping("/send-to-id")
    public ResponseEntity<?> sendMessageById(@RequestBody Map<String, Object> payload) {
        try {
            String rawSenderId    = payload.get("senderId").toString();
            String rawRecipientId = payload.get("recipientId").toString();
            String content        = payload.get("content").toString();

            String senderId    = rawSenderId.startsWith("T_") || rawSenderId.startsWith("S_")
                    ? rawSenderId : "T_" + rawSenderId;
            String recipientId = rawRecipientId.startsWith("T_") || rawRecipientId.startsWith("S_")
                    ? rawRecipientId : rawRecipientId;

            Message message = new Message(
                    senderId, recipientId,
                    "Chat", content, LocalDateTime.now());
            messageRepository.save(message);

            // Notify recipient
            String senderName = getSenderName(senderId);
            notificationService.createNotification(
                    recipientId,
                    "New message from " + senderName,
                    content.length() > 60 ? content.substring(0, 60) + "…" : content
            );

            return ResponseEntity.ok(Map.of("message", "Sent successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to send: " + e.getMessage()));
        }
    }

    // ── GET /api/messages/conversation ───────────────────────────────────────
    // Returns all messages in a conversation between two participants.
    // Used by ChatDetailScreen to load the full chat history from the server.
    // Params: teacherId (raw numeric), recipientId (with T_/S_ prefix)

    @GetMapping("/conversation")
    public ResponseEntity<List<Map<String, Object>>> getConversation(
            @RequestParam String teacherId,
            @RequestParam String recipientId) {
        String tid = "T_" + teacherId;
        List<Message> messages = messageRepository.findAll().stream()
                .filter(m ->
                        (m.getSenderId().equals(tid) && m.getRecipientId().equals(recipientId)) ||
                                (m.getSenderId().equals(recipientId) && m.getRecipientId().equals(tid))
                )
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(messages.stream().map(this::toMap).collect(Collectors.toList()));
    }

    // ── GET /api/messages/students/{teacherId} ────────────────────────────────
    // Returns list of students the teacher can message.
    // Used by the compose dialog to show a searchable student list.

    @GetMapping("/students/{teacherId}")
    public ResponseEntity<List<Map<String, Object>>> getStudentsForTeacher(
            @PathVariable String teacherId) {
        List<User> students = userRepository.findAll();
        List<Map<String, Object>> result = students.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",      s.getId());
            m.put("name",    s.getName());
            m.put("email",   s.getEmail());
            m.put("section", s.getSection());
            m.put("prefixedId", "S_" + s.getId());
            m.put("profilePictureUrl",
                    s.getProfilePictureUrl() != null ? s.getProfilePictureUrl() : "");
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ── Helper: convert Message to Flutter-friendly map ───────────────────────
    private Map<String, Object> toMap(Message m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id",          m.getId());
        map.put("senderId",    m.getSenderId());
        map.put("recipientId", m.getRecipientId());
        map.put("subject",     m.getSubject() != null ? m.getSubject() : "");
        map.put("content",     m.getContent());
        map.put("timestamp",   m.getTimestamp().toString());
        map.put("isRead",      m.isRead());
        return map;
    }

    // ── Helper: resolve display name from prefixed ID ─────────────────────────
    private String getSenderName(String prefixedId) {
        try {
            if (prefixedId.startsWith("T_")) {
                Long id = Long.parseLong(prefixedId.substring(2));
                return teacherRepository.findById(id)
                        .map(Teacher::getName).orElse("Teacher");
            } else if (prefixedId.startsWith("S_")) {
                Long id = Long.parseLong(prefixedId.substring(2));
                return userRepository.findById(id)
                        .map(User::getName).orElse("Student");
            }
        } catch (Exception ignored) {}
        return "User";
    }
}