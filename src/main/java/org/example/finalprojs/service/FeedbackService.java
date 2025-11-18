package org.example.finalprojs.service;

import org.example.finalprojs.model.Feedback;
import org.example.finalprojs.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    @Autowired
    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    public Feedback submitFeedback(Long userId, String commentText) {

        // Business Logic
        if (userId == null) {
            throw new IllegalArgumentException("User ID must be provided to submit feedback.");
        }

        if (commentText == null || commentText.trim().isEmpty()) {
            throw new IllegalArgumentException("The comment field cannot be empty.");
        }

        // Create the Feedback Entity
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setComment(commentText.trim());
        // RATING SETTING REMOVED
        feedback.setSubmittedAt(LocalDateTime.now());

        //Call the Repository to persist the data
        return feedbackRepository.save(feedback);
    }

}