package org.example.finalprojs;

import org.example.finalprojs.model.Message;
import org.example.finalprojs.model.User;
import org.example.finalprojs.model.RedeemItem;
import org.example.finalprojs.model.RedeemTransaction;
import org.example.finalprojs.model.GradeReport;
import org.example.finalprojs.repository.MessageRepository;
import org.example.finalprojs.repository.RedeemItemRepository;
import org.example.finalprojs.repository.RedeemTransactionRepository;
import org.example.finalprojs.repository.UserRepository;
import org.example.finalprojs.repository.GradeReportRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class RedeemController {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final RedeemItemRepository redeemItemRepository;
    private final GradeReportRepository gradeReportRepository;
    private final RedeemTransactionRepository redeemTransactionRepository;

    public RedeemController(MessageRepository messageRepository,
                            UserRepository userRepository,
                            RedeemItemRepository redeemItemRepository,
                            GradeReportRepository gradeReportRepository,
                            RedeemTransactionRepository redeemTransactionRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.redeemItemRepository = redeemItemRepository;
        this.gradeReportRepository = gradeReportRepository;
        this.redeemTransactionRepository = redeemTransactionRepository;
    }

    // --- Helper Method: Match Auth Logic from other Controllers ---
    private Optional<User> getCurrentUser(HttpSession session) {
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(userEmail);
    }

    // --- GET Mapping for Redeem Page ---
    @GetMapping("/redeem")
    public String viewRedeemPage(
            @RequestParam(required = false) String subjectName,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = userOptional.get();

        model.addAttribute("user", currentUser);
        List<Message> receivedMessages = messageRepository.findByRecipient(currentUser);
        long unreadCount = receivedMessages.stream().filter(m -> !m.isRead()).count();
        model.addAttribute("unreadCount", unreadCount);
        long sentCount = messageRepository.countBySender(currentUser);
        model.addAttribute("sentCount", sentCount);

        model.addAttribute("currentPoints", currentUser.getPoints());
        model.addAttribute("currentSubject", subjectName);

        if (subjectName == null || subjectName.isEmpty()) {
            model.addAttribute("error", "Please select a subject to view rewards.");
            model.addAttribute("items", new ArrayList<RedeemItem>());
        } else {
            List<RedeemItem> items = redeemItemRepository.findBySubject(subjectName);
            model.addAttribute("items", items);
        }

        model.addAttribute("pageTitle", "Redeem Rewards");
        return "redeem";
    }


    // --- POST Mapping to EXECUTE REDEMPTION (WITH STRICT WASTE CHECK) ---
    @PostMapping("/redeem/execute")
    @Transactional
    public String executeRedeem(
            @RequestParam Long redeemItemId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = userOptional.get();

        Optional<RedeemItem> itemOptional = redeemItemRepository.findById(redeemItemId);
        if (itemOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "The selected reward is invalid.");
            return "redirect:/redeem";
        }

        RedeemItem item = itemOptional.get();

        // 1. Check if user has enough points
        if (currentUser.getPoints() < item.getCost()) {
            redirectAttributes.addFlashAttribute("error", "Insufficient points to redeem " + item.getRewardName() + ".");
            return "redirect:/redeem?subjectName=" + item.getSubject();
        }

        Optional<GradeReport> reportOptional = gradeReportRepository.findByUserAndSubject(currentUser, item.getSubject());

        if (reportOptional.isPresent()) {
            GradeReport report = reportOptional.get();
            String targetAssessment = item.getTargetAssessment();
            int pointsAwarded = item.getPointsAwarded();

            // Get max and current scores from the GradeReport model
            int maxScore = report.getMaxScoreForAssessment(targetAssessment);
            int currentScore = report.getAssessmentScore(targetAssessment);

            int pointsNeeded = maxScore - currentScore;

            // --- CRITICAL CHECK A: Block redemption if already at max score ---
            if (currentScore >= maxScore) {
                redirectAttributes.addFlashAttribute("error",
                        "Redemption failed. The assessment '" + targetAssessment + "' for " + item.getSubject() + " is already at the maximum score of " + maxScore + ".");
                return "redirect:/redeem?subjectName=" + item.getSubject();
            }

            // --- CRITICAL CHECK B (STRICT): Block redemption if the reward awards more points than can be gained ---
            // If the item awards 3 points, but only 2 points are needed (8/10 score), block it.
            if (pointsNeeded < pointsAwarded) {
                String message = String.format(
                        "Redemption stopped: The reward '%s' awards %d points, but the score for '%s' is %d/%d. You only need %d more points. Redeeming this item would waste %d points of the reward.",
                        item.getRewardName(), pointsAwarded, targetAssessment, currentScore, maxScore, pointsNeeded, (pointsAwarded - pointsNeeded));

                redirectAttributes.addFlashAttribute("error", message);
                return "redirect:/redeem?subjectName=" + item.getSubject();
            }

            // --- Core Transaction Logic ---

            // 2. Deduct points from the user
            int newPoints = currentUser.getPoints() - item.getCost();
            currentUser.setPoints(newPoints);
            userRepository.save(currentUser);

            // 3. APPLY POINTS TO GRADE REPORT (This should now only run if pointsNeeded >= pointsAwarded)
            report.addPointsToAssessment(targetAssessment, pointsAwarded);
            gradeReportRepository.save(report);

            // 4. Show success message
            redirectAttributes.addFlashAttribute("success",
                    item.getRewardName() + " redeemed successfully! Points applied to your " + item.getSubject() + " report.");

        } else {
            // If no report exists, but user has points, deduct points and warn them.
            int newPoints = currentUser.getPoints() - item.getCost();
            currentUser.setPoints(newPoints);
            userRepository.save(currentUser);

            redirectAttributes.addFlashAttribute("warning",
                    item.getRewardName() + " redeemed, but a grade report for " + item.getSubject() + " could not be found to apply the points.");
        }

        // 5. CREATE AND SAVE THE TRANSACTION RECORD
        RedeemTransaction transaction = new RedeemTransaction();
        transaction.setUser(currentUser);
        transaction.setRedeemItem(item);

        redeemTransactionRepository.save(transaction);

        // Redirect back to the subject-specific redeem page
        return "redirect:/redeem?subjectName=" + item.getSubject();
    }
}