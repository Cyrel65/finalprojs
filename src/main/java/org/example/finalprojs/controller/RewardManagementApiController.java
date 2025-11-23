package org.example.finalprojs.controller;

import org.example.finalprojs.model.GradeReport; // NEW IMPORT
import org.example.finalprojs.model.RedeemItem;
import org.example.finalprojs.repository.RedeemItemRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/teacher/rewards")
public class RewardManagementApiController {

    private final RedeemItemRepository redeemItemRepository;

    @Autowired
    public RewardManagementApiController(RedeemItemRepository redeemItemRepository) {
        this.redeemItemRepository = redeemItemRepository;
    }

    // 1. Endpoint for creating a new reward (POST)
    @PostMapping
    public ResponseEntity<RedeemItem> createRewardOption(@RequestBody RedeemItem newItem) {
        RedeemItem savedItem = redeemItemRepository.save(newItem);
        return new ResponseEntity<>(savedItem, HttpStatus.CREATED);
    }

    // 2. Endpoint for retrieving all reward options (GET)
    @GetMapping
    public List<RedeemItem> getAllRewardOptions() {
        return redeemItemRepository.findAll();
    }

    // 3. NEW Endpoint for retrieving valid assessment field names (GET /assessments)
    @GetMapping("/assessments")
    public ResponseEntity<List<String>> getAssessmentFieldNames() {
        // Use the static method in GradeReport model to dynamically get field names
        List<String> assessmentNames = GradeReport.getAssessmentFieldNames();
        return new ResponseEntity<>(assessmentNames, HttpStatus.OK);
    }
}