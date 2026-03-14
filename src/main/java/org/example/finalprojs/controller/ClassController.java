package org.example.finalprojs.controller;

import org.example.finalprojs.model.Teacher;
import org.example.finalprojs.model.TeacherClass;
import org.example.finalprojs.model.User;
import org.example.finalprojs.repository.TeacherClassRepository;
import org.example.finalprojs.repository.UserRepository;
import org.example.finalprojs.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/classes")
@CrossOrigin(origins = "*")
public class ClassController {

    private final TeacherClassRepository teacherClassRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;

    @Autowired
    public ClassController(TeacherClassRepository teacherClassRepository,
                           UserRepository userRepository,
                           TeacherRepository teacherRepository) {
        this.teacherClassRepository = teacherClassRepository;
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createClass(@RequestBody Map<String, Object> payload) {
        try {
            Long teacherId = Long.valueOf(payload.get("teacherId").toString());
            String subject = (String) payload.get("subject");
            String section = (String) payload.get("section");
            String startTime = (String) payload.get("startTime");

            Optional<Teacher> teacherOpt = teacherRepository.findById(teacherId);
            if (teacherOpt.isPresent()) {
                TeacherClass newClass = new TeacherClass();
                newClass.setTeacher(teacherOpt.get());
                newClass.setSubject(subject);
                newClass.setSection(section);
                newClass.setStartTime(startTime);
                teacherClassRepository.save(newClass);
                return ResponseEntity.ok(Map.of("message", "Class created successfully"));
            }
            return ResponseEntity.status(404).body(Map.of("message", "Teacher not found"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/add-student")
    public ResponseEntity<?> addStudentToSection(@RequestBody Map<String, String> payload) {
        String studentEmail = payload.get("email");
        String targetSection = payload.get("section");

        Optional<User> studentOpt = userRepository.findByEmail(studentEmail);
        if (studentOpt.isPresent()) {
            User student = studentOpt.get();
            student.setSection(targetSection);
            userRepository.save(student);
            return ResponseEntity.ok(Map.of("message", "Student added to section " + targetSection));
        }
        return ResponseEntity.status(404).body(Map.of("message", "Student not found"));
    }
}