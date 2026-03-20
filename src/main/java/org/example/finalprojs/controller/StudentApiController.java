package org.example.finalprojs.controller;

import org.example.finalprojs.model.User;
import org.example.finalprojs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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

    // Returns all students currently enrolled in a section
    @GetMapping("/section/{section}")
    public ResponseEntity<List<User>> getStudentsBySection(@PathVariable String section) {
        List<User> students = userRepository.findBySection(section);
        return ResponseEntity.ok(students);
    }

    // FIX: Enroll an existing DB user into a section by email only.
    // NEVER creates a new user — looks up the email in users table.
    // Flutter sends: { "email": "...", "section": "..." }
    // Returns 404 if email not found → Flutter shows inline error in dialog.
    @PostMapping("/save")
    public ResponseEntity<?> enrollStudent(@RequestBody Map<String, Object> payload) {
        String email   = (String) payload.get("email");
        String section = (String) payload.get("section");

        if (email == null || section == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email and section are required."));
        }

        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());

        if (userOpt.isEmpty()) {
            // Returns 404 → Flutter dialog stays open and shows the error inline
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message",
                            "No account found for \"" + email + "\". " +
                                    "The student must register on the webapp first."));
        }

        User student = userOpt.get();

        if (section.equals(student.getSection())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message",
                            "Student is already enrolled in " + section + "."));
        }

        student.setSection(section);
        userRepository.save(student);
        return ResponseEntity.ok(Map.of("message", "Student enrolled in " + section));
    }

    // FIX: Unenroll a student from their section by setting section = null.
    // Does NOT delete the user account → no foreign key error with grade_reports.
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> unenrollStudent(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Student not found."));
        }

        User student = userOpt.get();
        student.setSection(null); // Remove from class — account and grades stay intact
        userRepository.save(student);

        return ResponseEntity.ok(Map.of("message", "Student unenrolled successfully."));
    }
}