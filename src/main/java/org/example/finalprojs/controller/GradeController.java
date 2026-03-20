package org.example.finalprojs.controller;

import org.example.finalprojs.model.GradeReport;
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

    @GetMapping("/rankings/{section}")
    public ResponseEntity<List<Map<String, Object>>> getRankings(@PathVariable String section) {
        List<Map<String, Object>> rankings = gradeService.getRankingsBySection(section);
        return ResponseEntity.ok(rankings);
    }

    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> getGradeReport(
            @RequestParam String email,
            @RequestParam String subject) {
        try {
            Map<String, Object> report = gradeService.getGradeReport(email, subject);
            return ResponseEntity.ok(report);
        } catch (RuntimeException e) {
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

    // ─── NEW: This was the missing endpoint causing scores not to save ────────
    @PostMapping("/update")
    public ResponseEntity<?> updateGrade(@RequestBody Map<String, Object> payload) {
        try {
            if (payload.get("userId") == null || payload.get("subject") == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "userId and subject are required"));
            }

            Long userId = Long.valueOf(payload.get("userId").toString());
            String subject = (String) payload.get("subject");

            GradeReport saved = gradeService.updateGrade(userId, subject, payload);

            return ResponseEntity.ok(Map.of(
                    "message", "Grade updated successfully",
                    "gradeId", saved.getId()
            ));

        } catch (RuntimeException e) {
            System.err.println("[GradeController] updateGrade failed: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Update failed: " + e.getMessage()));
        }
    }
}