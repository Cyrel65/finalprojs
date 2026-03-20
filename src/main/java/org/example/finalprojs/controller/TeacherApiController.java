package org.example.finalprojs.controller;

import org.example.finalprojs.model.Teacher;
import org.example.finalprojs.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/teachers")
@CrossOrigin(origins = "*")
public class TeacherApiController {

    private final TeacherRepository teacherRepository;

    // Profile pictures are saved here and served as static resources.
    // Make sure Spring Boot serves /uploads/** as static content (see below).
    private static final String UPLOAD_DIR = "uploads/profiles/";

    @Autowired
    public TeacherApiController(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    // ── GET /api/teachers/{id} ────────────────────────────────────────────────
    // Called by Flutter on profile screen load to get current profile picture URL.

    @GetMapping("/{id}")
    public ResponseEntity<?> getTeacher(@PathVariable Long id) {
        return teacherRepository.findById(id)
                .<ResponseEntity<?>>map(teacher -> ResponseEntity.ok(java.util.Map.of(
                        "id",                teacher.getId(),
                        "name",              teacher.getName(),
                        "email",             teacher.getEmail(),
                        "profilePictureUrl", teacher.getProfilePictureUrl() != null
                                ? teacher.getProfilePictureUrl() : ""
                )))
                .orElse(ResponseEntity.status(404)
                        .body(java.util.Map.of("message", "Teacher not found")));
    }

    // ── POST /api/teachers/{id}/upload-profile ────────────────────────────────
    // Called by Flutter profile screen when teacher picks a new profile picture.
    // Saves the image to disk and stores the URL path in the Teacher entity.

    @PostMapping("/{id}/upload-profile")
    public ResponseEntity<?> uploadProfilePicture(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            Optional<Teacher> teacherOpt = teacherRepository.findById(id);
            if (teacherOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(Map.of("message", "Teacher not found with id: " + id));
            }

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "No file provided"));
            }

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename to avoid collisions
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String uniqueFilename = "teacher_" + id + "_" + UUID.randomUUID() + extension;

            // Save file to disk
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.write(filePath, file.getBytes());

            // Store the accessible URL path in DB
            // This assumes Spring Boot serves /uploads/** as static resources
            String imageUrl = "/uploads/profiles/" + uniqueFilename;

            Teacher teacher = teacherOpt.get();
            teacher.setProfilePictureUrl(imageUrl);
            teacherRepository.save(teacher);

            return ResponseEntity.ok(Map.of(
                    "message",  "Profile picture uploaded successfully",
                    "imageUrl", imageUrl
            ));

        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to upload image: " + e.getMessage()));
        }
    }

    // ── PUT /api/teachers/update/{id} ─────────────────────────────────────────
    // Called by Flutter profile screen when teacher updates name/email/password.

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateProfile(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        try {
            Optional<Teacher> teacherOpt = teacherRepository.findById(id);
            if (teacherOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(Map.of("message", "Teacher not found"));
            }

            Teacher teacher = teacherOpt.get();

            // Verify current password
            String currentPassword = payload.get("currentPassword");
            if (currentPassword == null || !teacher.getPassword().equals(currentPassword)) {
                return ResponseEntity.status(401)
                        .body(Map.of("message", "Current password is incorrect"));
            }

            // Update name and email
            if (payload.get("name") != null) teacher.setName(payload.get("name"));
            if (payload.get("email") != null) teacher.setEmail(payload.get("email"));

            // Update password if provided
            String newPassword = payload.get("newPassword");
            if (newPassword != null && !newPassword.isEmpty()) {
                teacher.setPassword(newPassword);
            }

            teacherRepository.save(teacher);
            return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Update failed: " + e.getMessage()));
        }
    }
}