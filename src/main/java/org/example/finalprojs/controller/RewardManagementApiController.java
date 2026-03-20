package org.example.finalprojs.controller;

import org.example.finalprojs.model.GradeReport;
import org.example.finalprojs.model.RedeemItem;
import org.example.finalprojs.repository.RedeemItemRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/teacher/rewards")
@CrossOrigin(origins = "*")
public class RewardManagementApiController {

    private final RedeemItemRepository redeemItemRepository;

    @Autowired
    public RewardManagementApiController(RedeemItemRepository redeemItemRepository) {
        this.redeemItemRepository = redeemItemRepository;
    }

    // 1. Create a new reward (POST)
    @PostMapping
    public ResponseEntity<RedeemItem> createRewardOption(@RequestBody RedeemItem newItem) {
        RedeemItem savedItem = redeemItemRepository.save(newItem);
        return new ResponseEntity<>(savedItem, HttpStatus.CREATED);
    }

    // 2. Get all reward options (GET)
    @GetMapping
    public List<RedeemItem> getAllRewardOptions() {
        return redeemItemRepository.findAll();
    }

    // 3. Get valid assessment field names (GET /assessments)
    @GetMapping("/assessments")
    public ResponseEntity<List<String>> getAssessmentFieldNames() {
        List<String> assessmentNames = GradeReport.getAssessmentFieldNames();
        return new ResponseEntity<>(assessmentNames, HttpStatus.OK);
    }

    // 4. NEW: Delete a reward by ID (DELETE /{id})
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReward(@PathVariable Long id) {
        Optional<RedeemItem> item = redeemItemRepository.findById(id);
        if (item.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Reward not found with id: " + id);
        }
        redeemItemRepository.deleteById(id);
        return ResponseEntity.ok("Reward deleted successfully");
    }
}