package org.example.finalprojs.controller;

import org.example.finalprojs.model.User;
import org.example.finalprojs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentApiController {

    private final UserRepository userRepository;

    @Autowired
    public StudentApiController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/section/{section}")
    public ResponseEntity<List<User>> getStudentsBySection(@PathVariable String section) {
        List<User> students = userRepository.findBySection(section);
        return ResponseEntity.ok(students);
    }

    @PostMapping("/save")
    public ResponseEntity<User> saveStudent(@RequestBody User student) {
        try {
            // Spring Data JPA's .save() handles both INSERT (if id is null)
            // and UPDATE (if id exists in the database).
            User savedStudent = userRepository.save(student);
            return new ResponseEntity<>(savedStudent, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<HttpStatus> deleteStudent(@PathVariable Long id) {
        try {
            userRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}