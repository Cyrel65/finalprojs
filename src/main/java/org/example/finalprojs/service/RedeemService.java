package org.example.finalprojs.service;

import org.example.finalprojs.model.User;
import org.example.finalprojs.model.RedeemItem;
import org.example.finalprojs.model.RedeemTransaction;
import org.example.finalprojs.model.GradeReport;
import org.example.finalprojs.model.StudentPoints;
import org.example.finalprojs.repository.GradeReportRepository;
import org.example.finalprojs.repository.RedeemItemRepository;
import org.example.finalprojs.repository.RedeemTransactionRepository;
import org.example.finalprojs.repository.StudentPointsRepository;
import org.example.finalprojs.repository.UserRepository;
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
    private final StudentPointsRepository studentPointsRepository;

    @Autowired
    public RedeemService(UserRepository userRepository,
                         RedeemItemRepository redeemItemRepository,
                         GradeReportRepository gradeReportRepository,
                         RedeemTransactionRepository redeemTransactionRepository,
                         StudentPointsRepository studentPointsRepository) {
        this.userRepository              = userRepository;
        this.redeemItemRepository        = redeemItemRepository;
        this.gradeReportRepository       = gradeReportRepository;
        this.redeemTransactionRepository = redeemTransactionRepository;
        this.studentPointsRepository     = studentPointsRepository;
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<RedeemItem> findRedeemItemById(Long redeemItemId) {
        return redeemItemRepository.findById(redeemItemId);
    }

    /**
     * Returns only rewards for this exact subject + section.
     * A reward for "ITIM 2 - CP" will NOT appear for "ITIM 2 - BN".
     */
    public List<RedeemItem> findRedeemItemsBySubjectAndSection(
            String subject, String section) {
        return redeemItemRepository.findBySubjectAndSection(subject, section);
    }

    public List<RedeemTransaction> findTransactionsByUser(User user) {
        return redeemTransactionRepository
                .findAllByUserOrderByRedeemDateDesc(user);
    }

    public int getPointsForSubjectAndSection(
            User user, String subject, String section) {
        return studentPointsRepository
                .findByUserAndSubjectAndSection(user, subject, section)
                .map(StudentPoints::getPoints)
                .orElse(0);
    }

    @Transactional
    public void executeRedemption(
            User currentUser, Long redeemItemId, String section) {

        RedeemItem item = findRedeemItemById(redeemItemId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "The selected reward is invalid."));

        // Guard: reward must belong to the student's section
        if (item.getSection() != null && !item.getSection().isEmpty()
                && !item.getSection().equalsIgnoreCase(section)) {
            throw new IllegalArgumentException(
                    "This reward is not available for your section.");
        }

        StudentPoints balance = studentPointsRepository
                .findByUserAndSubjectAndSection(
                        currentUser, item.getSubject(), section)
                .orElseThrow(() -> new IllegalArgumentException(
                        "You have no points for "
                                + item.getSubject() + " - " + section + "."));

        if (balance.getPoints() < item.getCost()) {
            throw new IllegalArgumentException(
                    "Insufficient points. You have " + balance.getPoints()
                            + " pts for " + item.getSubject()
                            + " but need " + item.getCost() + ".");
        }

        Optional<GradeReport> reportOptional =
                gradeReportRepository.findByUserAndSubject(
                        currentUser, item.getSubject());

        if (reportOptional.isPresent()) {
            GradeReport report      = reportOptional.get();
            String targetAssessment = item.getTargetAssessment();
            int pointsAwarded       = item.getPointsAwarded();
            int maxScore            = report.getMaxScoreForAssessment(targetAssessment);
            int currentScore        = report.getAssessmentScore(targetAssessment);
            int pointsNeeded        = maxScore - currentScore;

            if (currentScore >= maxScore) {
                throw new IllegalArgumentException(
                        "'" + targetAssessment
                                + "' is already at max score of " + maxScore + ".");
            }
            if (pointsNeeded < pointsAwarded) {
                throw new IllegalArgumentException(String.format(
                        "'%s' awards %d pts but you only need %d more. "
                                + "Redeeming would waste %d pts.",
                        item.getRewardName(), pointsAwarded,
                        pointsNeeded, pointsAwarded - pointsNeeded));
            }

            balance.setPoints(balance.getPoints() - item.getCost());
            studentPointsRepository.save(balance);

            report.addPointsToAssessment(targetAssessment, pointsAwarded);
            gradeReportRepository.save(report);

        } else {
            balance.setPoints(balance.getPoints() - item.getCost());
            studentPointsRepository.save(balance);
        }

        RedeemTransaction transaction = new RedeemTransaction();
        transaction.setUser(currentUser);
        transaction.setRedeemItem(item);
        redeemTransactionRepository.save(transaction);
    }
}