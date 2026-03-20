package org.example.finalprojs.controller;

import org.example.finalprojs.model.User;
import org.example.finalprojs.repository.GradeReportRepository;
import org.example.finalprojs.repository.RedeemTransactionRepository;
import org.example.finalprojs.repository.StudentPointsRepository;
import org.example.finalprojs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentApiController {

    private final UserRepository userRepository;
    private final GradeReportRepository gradeReportRepository;
    private final StudentPointsRepository studentPointsRepository;
    private final RedeemTransactionRepository redeemTransactionRepository;

    @Autowired
    public StudentApiController(UserRepository userRepository,
                                GradeReportRepository gradeReportRepository,
                                StudentPointsRepository studentPointsRepository,
                                RedeemTransactionRepository redeemTransactionRepository) {
        this.userRepository             = userRepository;
        this.gradeReportRepository      = gradeReportRepository;
        this.studentPointsRepository    = studentPointsRepository;
        this.redeemTransactionRepository = redeemTransactionRepository;
    }

    @GetMapping("/section/{section}")
    public ResponseEntity<List<User>> getStudentsBySection(@PathVariable String section) {
        List<User> students = userRepository.findBySection(section);
        return ResponseEntity.ok(students);
    }

    @PostMapping("/save")
    public ResponseEntity<User> saveStudent(@RequestBody User student) {
        try {
            User savedStudent = userRepository.save(student);
            return new ResponseEntity<>(savedStudent, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ── DELETE with cascade ───────────────────────────────────────────────────
    // Deletes all related records first to avoid foreign key constraint errors,
    // then deletes the student (User) record.

    @DeleteMapping("/delete/{id}")
    @Transactional
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        try {
            Optional<User> studentOpt = userRepository.findById(id);
            if (studentOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Student not found with id: " + id));
            }

            User student = studentOpt.get();

            // 1. Delete grade reports
            gradeReportRepository.deleteAll(
                    gradeReportRepository.findByUser(student));

            // 2. Delete per-subject points
            studentPointsRepository.deleteAll(
                    studentPointsRepository.findByUser(student));

            // 3. Delete redeem transactions
            redeemTransactionRepository.deleteAll(
                    redeemTransactionRepository.findByUser(student));

            // 4. Finally delete the student
            userRepository.deleteById(id);

            return ResponseEntity.ok(
                    Map.of("message", "Student and all related data deleted successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to delete student: " + e.getMessage()));
        }
    }
}