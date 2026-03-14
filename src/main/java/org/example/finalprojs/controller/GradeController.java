package org.example.finalprojs.controller;

import org.example.finalprojs.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grades")
@CrossOrigin(origins = "*")
public class GradeController {

    private final GradeService gradeService;

    @Autowired
    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    /**
     * GET /api/grades/rankings/{section}
     * Used by Flutter StudentRankingsScreen.
     * Returns all students in a section sorted by overall grade descending.
     */
    @GetMapping("/rankings/{section}")
    public ResponseEntity<List<Map<String, Object>>> getRankings(@PathVariable String section) {
        List<Map<String, Object>> rankings = gradeService.getRankingsBySection(section);
        return ResponseEntity.ok(rankings);
    }

    /**
     * GET /api/grades/report?email=john@email.com&subject=Math
     * Used by Flutter ClassDetailScreen on every load.
     * Returns the full grade breakdown for one student + subject so grades
     * are always in sync with the database when the teacher revisits a class.
     *
     * FIX: The previous version used Map.of() with more than 10 key-value pairs,
     * which exceeds the overload limit of Map.of() in Java (max 10 pairs).
     * Fixed by using a LinkedHashMap instead.
     */
    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> getGradeReport(
            @RequestParam String email,
            @RequestParam String subject) {
        try {
            Map<String, Object> report = gradeService.getGradeReport(email, subject);
            return ResponseEntity.ok(report);
        } catch (RuntimeException e) {
            // Student has no grades saved yet — return all zeros so Flutter
            // populates the form with empty/zero values instead of crashing.
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("selfCheck1",   0);
            empty.put("selfCheck2",   0);
            empty.put("selfCheck3",   0);
            empty.put("selfCheck4",   0);
            empty.put("selfCheck5",   0);
            empty.put("taskSheet1",   0);
            empty.put("taskSheet2",   0);
            empty.put("taskSheet3",   0);
            empty.put("unitTest1",    0);
            empty.put("unitTest2",    0);
            empty.put("termTest",     0);
            empty.put("attendance",   0);
            empty.put("overallGrade", 0.0);
            return ResponseEntity.ok(empty);
        }
    }
}