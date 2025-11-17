package org.example.finalprojs.service;

import org.example.finalprojs.model.User;
import org.example.finalprojs.model.RedeemItem;
import org.example.finalprojs.model.RedeemTransaction; // IMPORTANT: Ensure this is imported
import org.example.finalprojs.model.GradeReport;
import org.example.finalprojs.repository.RedeemItemRepository;
import org.example.finalprojs.repository.RedeemTransactionRepository; // IMPORTANT: Used for new method
import org.example.finalprojs.repository.UserRepository;
import org.example.finalprojs.repository.GradeReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RedeemService {

    private final UserRepository userRepository;
    private final RedeemItemRepository redeemItemRepository;
    private final GradeReportRepository gradeReportRepository;
    private final RedeemTransactionRepository redeemTransactionRepository;

    @Autowired
    public RedeemService(UserRepository userRepository,
                         RedeemItemRepository redeemItemRepository,
                         GradeReportRepository gradeReportRepository,
                         RedeemTransactionRepository redeemTransactionRepository) {
        this.userRepository = userRepository;
        this.redeemItemRepository = redeemItemRepository;
        this.gradeReportRepository = gradeReportRepository;
        this.redeemTransactionRepository = redeemTransactionRepository;
    }

    // --- Retrieval Methods ---

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<RedeemItem> findRedeemItemById(Long redeemItemId) {
        return redeemItemRepository.findById(redeemItemId);
    }

    public List<RedeemItem> findRedeemItemsBySubject(String subjectName) {
        return redeemItemRepository.findBySubject(subjectName);
    }

    /**
     * Finds all redemption transactions for a specific user, ordered by date.
     * This resolves the 'Cannot resolve method' error in the controller.
     * @param user The user whose history is requested.
     * @return A list of RedeemTransaction objects.
     */
    public List<RedeemTransaction> findTransactionsByUser(User user) {
        // ASSUMPTION: The RedeemTransactionRepository has a method defined as:
        // List<RedeemTransaction> findAllByUserOrderByRedeemDateDesc(User user);
        return redeemTransactionRepository.findAllByUserOrderByRedeemDateDesc(user);
    }

    // --- Core Transaction Logic ---

    /**
     * Executes the point redemption, including all validation, score checks,
     * point deduction, grade report update, and transaction recording.
     * @throws IllegalArgumentException if validation fails (e.g., insufficient points, score maxed, points wasted).
     */
    @Transactional
    public void executeRedemption(User currentUser, Long redeemItemId) {
        RedeemItem item = findRedeemItemById(redeemItemId)
                .orElseThrow(() -> new IllegalArgumentException("The selected reward is invalid."));

        // 1. Check if user has enough points
        if (currentUser.getPoints() < item.getCost()) {
            throw new IllegalArgumentException("Insufficient points to redeem " + item.getRewardName() + ".");
        }

        // 2. Fetch the target GradeReport
        Optional<GradeReport> reportOptional = gradeReportRepository.findByUserAndSubject(currentUser, item.getSubject());

        if (reportOptional.isPresent()) {
            GradeReport report = reportOptional.get();
            String targetAssessment = item.getTargetAssessment();
            int pointsAwarded = item.getPointsAwarded();

            // Get max and current scores (Assumes these helper methods exist on GradeReport)
            int maxScore = report.getMaxScoreForAssessment(targetAssessment);
            int currentScore = report.getAssessmentScore(targetAssessment);

            int pointsNeeded = maxScore - currentScore;

            // CRITICAL CHECK A: Block redemption if already at max score
            if (currentScore >= maxScore) {
                throw new IllegalArgumentException(
                        "Redemption failed. The assessment '" + targetAssessment + "' for " + item.getSubject() + " is already at the maximum score of " + maxScore + ".");
            }

            // CRITICAL CHECK B (STRICT): Block redemption if the reward awards more points than can be gained
            if (pointsNeeded < pointsAwarded) {
                int wastedPoints = pointsAwarded - pointsNeeded;
                String message = String.format(
                        "Redemption stopped: The reward '%s' awards %d points, but you only need %d more points. Redeeming this item would waste %d points of the reward.",
                        item.getRewardName(), pointsAwarded, pointsNeeded, wastedPoints);

                throw new IllegalArgumentException(message);
            }

            // --- Core Transaction Success ---

            // 3. Deduct points from the user
            int newPoints = currentUser.getPoints() - item.getCost();
            currentUser.setPoints(newPoints);
            userRepository.save(currentUser); // Update user points

            // 4. APPLY POINTS TO GRADE REPORT (Only runs if pointsNeeded >= pointsAwarded)
            report.addPointsToAssessment(targetAssessment, pointsAwarded);
            gradeReportRepository.save(report); // Update report

        } else {
            // Case where no report exists, but item is valid (deduct points and warn)

            // 3. Deduct points from the user
            int newPoints = currentUser.getPoints() - item.getCost();
            currentUser.setPoints(newPoints);
            userRepository.save(currentUser); // Update user points

            // Note: Since we throw an exception for insufficient points above, this handles only the case
            // where the report is missing. We will handle the warning message in the controller.
        }

        // 5. CREATE AND SAVE THE TRANSACTION RECORD
        RedeemTransaction transaction = new RedeemTransaction();
        transaction.setUser(currentUser);
        transaction.setRedeemItem(item);
        redeemTransactionRepository.save(transaction);
    }
}