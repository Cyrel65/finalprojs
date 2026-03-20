package org.example.finalprojs.controller;

import org.example.finalprojs.model.AppNotification;
import org.example.finalprojs.service.AppNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationApiController {

    private final AppNotificationService notificationService;

    @Autowired
    public NotificationApiController(AppNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // GET /api/notifications/teacher/{teacherId}
    // Called by Flutter to load notifications for a teacher.
    // teacherId is the raw numeric ID (Flutter sends it without prefix).
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<Map<String, Object>>> getTeacherNotifications(
            @PathVariable String teacherId) {
        String recipientId = "T_" + teacherId;
        return ResponseEntity.ok(toMapList(notificationService.getNotifications(recipientId)));
    }

    // POST /api/notifications/read/{id}
    @PostMapping("/read/{id}")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        boolean success = notificationService.markAsRead(id);
        return success
                ? ResponseEntity.ok(Map.of("message", "Marked as read"))
                : ResponseEntity.status(404).body(Map.of("message", "Notification not found"));
    }

    // POST /api/notifications/read-all/{teacherId}
    @PostMapping("/read-all/{teacherId}")
    public ResponseEntity<?> markAllAsRead(@PathVariable String teacherId) {
        notificationService.markAllAsRead("T_" + teacherId);
        return ResponseEntity.ok(Map.of("message", "All marked as read"));
    }

    // DELETE /api/notifications/delete/{id}
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        boolean success = notificationService.deleteNotification(id);
        return success
                ? ResponseEntity.ok(Map.of("message", "Deleted"))
                : ResponseEntity.status(404).body(Map.of("message", "Not found"));
    }

    // ── Helper: convert AppNotification to Flutter-friendly map ──────────────
    // Flutter's NotificationItem.fromMap() expects:
    // id, name, message, time, date, isRead
    private List<Map<String, Object>> toMapList(List<AppNotification> list) {
        return list.stream().map(n -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id",      n.getId());
            map.put("name",    n.getName());
            map.put("message", n.getMessage());
            map.put("time",    n.getTime());
            map.put("date",    n.getDate());
            map.put("isRead",  n.isRead());
            return map;
        }).collect(Collectors.toList());
    }
}