package org.example.finalprojs.controller;

import org.example.finalprojs.model.Teacher;
import org.example.finalprojs.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final TeacherService teacherService;

    @Autowired
    public AuthController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Teacher teacher) {
        try {
            Teacher registeredTeacher = teacherService.register(teacher);
            return new ResponseEntity<>(registeredTeacher, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        Optional<Teacher> teacher = teacherService.login(email, password);

        if (teacher.isPresent()) {
            return ResponseEntity.ok(teacher.get());
        } else {
            return new ResponseEntity<>(Map.of("message", "Invalid email or password"), HttpStatus.UNAUTHORIZED);
        }
    }
}