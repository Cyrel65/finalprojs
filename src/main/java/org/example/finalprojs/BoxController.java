package org.example.finalprojs;

import jakarta.servlet.http.HttpSession;
import org.example.finalprojs.model.Box;
import org.example.finalprojs.model.RedeemTransaction;
import org.example.finalprojs.model.User;
import org.example.finalprojs.repository.BoxRepository;
import org.example.finalprojs.repository.RedeemTransactionRepository;
import org.example.finalprojs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class BoxController {

    @Autowired
    private BoxRepository boxRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedeemTransactionRepository redeemTransactionRepository;

    // Placeholder for the current user's ID (Teacher access assumed)
    private static final Long CURRENT_USER_ID = 1L;

    // --- 1. Dashboard GET Mapping (To Display Boxes) ---
    @GetMapping("/")
    public String viewDashboard(Model model) {

        List<Box> allBoxes = boxRepository.findAll();
        model.addAttribute("boxes", allBoxes);

        return "index";
    }

    // --- 2. Box Creation POST Mapping (Teacher functionality) ---
    @PostMapping("/createBox")
    public String createNewBox(@RequestParam int points,
                               @RequestParam String typeOfTest,
                               RedirectAttributes redirectAttributes) {

        // NOTE: This uses the hardcoded ID for simplicity.
        User currentUser = userRepository.findById(CURRENT_USER_ID)
                .orElse(null);

        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "System error: Default user not found.");
            return "redirect:/login";
        }

        Box newBox = new Box(points, typeOfTest, currentUser);
        boxRepository.save(newBox);

        return "redirect:/redeem";
    }

    // --- 3. Redeem Execution POST Mapping (Core Point Deduction) ---
    // --- 3. Redeem Execution POST Mapping (The core point deduction logic) ---
    @PostMapping("/redeem/execute")
    @Transactional
    public String executeRedeem(@RequestParam Long boxId, HttpSession session, RedirectAttributes redirectAttributes) {

        // 1. Authentication Check using HttpSession
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) {
            redirectAttributes.addFlashAttribute("error", "Authentication failed. Please log in to redeem items.");
            return "redirect:/login";
        }

        // 2. Fetch the current User by email
        Optional<User> userOptional = userRepository.findByEmail(userEmail);

        if (userOptional.isEmpty()) {
            session.invalidate(); // Clear stale session
            redirectAttributes.addFlashAttribute("error", "User session error: Account not found.");
            return "redirect:/login";
        }
        User user = userOptional.get();

        // 3. Get the Box
        Optional<Box> boxOptional = boxRepository.findById(boxId);
        if (boxOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Error: The selected item could not be found.");
            return "redirect:/redeem";
        }
        Box boxToRedeem = boxOptional.get();


        // 4. CRITICAL BUSINESS LOGIC: Check Points (Updated Error Message)
        if (user.getPoints() < boxToRedeem.getPoints()) {
            int required = boxToRedeem.getPoints();
            int available = user.getPoints();
            int needed = required - available;

            // Detailed Error Alert Message
            redirectAttributes.addFlashAttribute("error",
                    "Redemption failed: Insufficient points. You need " + required + " points, but only have " + available + ". You are short " + needed + " points."
            );
            return "redirect:/redeem";
        }

        // 5. Record the Transaction
        RedeemTransaction transaction = new RedeemTransaction();
        transaction.setBox(boxToRedeem);
        transaction.setUser(user);
        transaction.setRedeemDate(LocalDateTime.now());
        redeemTransactionRepository.save(transaction); // SAVES HISTORY

        // 6. DEDUCT POINTS and SAVE UPDATED USER
        int newPoints = user.getPoints() - boxToRedeem.getPoints();
        user.setPoints(newPoints);
        userRepository.save(user); // UPDATES USER POINTS

        // Detailed Success Alert Message
        redirectAttributes.addFlashAttribute("success",
                "Success! You redeemed '" + boxToRedeem.getTypeOfTest() + "'. A total of " + boxToRedeem.getPoints() + " points was deducted. Your new balance is " + user.getPoints() + " points."
        );
        return "redirect:/redeem";
    }

    // --- 4. ADDED: Redeem Page GET Mapping (Loads the main redemption view) ---
    @GetMapping("/redeem")
    public String viewWidgets(Model model, HttpSession session) {

        // Use the email from the session, matching your custom authentication
        String userEmail = (String) session.getAttribute("userEmail");

        if (userEmail == null) {
            return "redirect:/login";
        }

        // Fetch User and Safely Handle Optional
        Optional<User> userOptional = userRepository.findByEmail(userEmail);

        if (userOptional.isEmpty()) {
            session.invalidate();
            return "redirect:/login";
        }

        User currentUser = userOptional.get();

        // Add the user's point total to the model
        model.addAttribute("currentPoints", currentUser.getPoints());

        // Also fetch the boxes list
        List<Box> allBoxes = boxRepository.findAll();
        model.addAttribute("boxes", allBoxes);

        return "widgets";
    }

    // --- 5. Redeem History GET Mapping (Completes the history feature) ---
    @GetMapping("/redeem/history")
    public String viewRedeemHistory(Model model, HttpSession session) {

        String userEmail = (String) session.getAttribute("userEmail");

        if (userEmail == null) {
            return "redirect:/login";
        }

        // 1. Fetch User
        Optional<User> userOptional = userRepository.findByEmail(userEmail);

        if (userOptional.isEmpty()) {
            session.invalidate();
            return "redirect:/login";
        }

        User user = userOptional.get();

        // 2. Fetch all transactions for this user, ordered by date
        List<RedeemTransaction> history = redeemTransactionRepository.findByUserOrderByRedeemDateDesc(user);

        model.addAttribute("history", history);

        return "redeem_history"; // Your history template
    }
}