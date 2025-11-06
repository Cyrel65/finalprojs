package org.example.finalprojs;

import org.example.finalprojs.model.Box;
import org.example.finalprojs.model.User;
import org.example.finalprojs.model.Message;
import org.example.finalprojs.model.Score; // NEW: Import Score Model
import org.example.finalprojs.repository.BoxRepository;
import org.example.finalprojs.repository.MessageRepository;
import org.example.finalprojs.repository.ScoreRepository; // NEW: Import Score Repository
import org.example.finalprojs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
public class controller {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BoxRepository boxRepository;


    @Autowired
    private MessageRepository messageRepository;

    // NEW: Inject the Score Repository for personalized scores view
    @Autowired
    private ScoreRepository scoreRepository;


    // Private Helper Method for Session User
    private Optional<User> getCurrentUser(HttpSession session) {
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(userEmail);
    }

    // --- Registration Logic ---

    @GetMapping("/register")
    public String viewRegister(Model model) {
        model.addAttribute("user", new User());
        return "page-register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user) {
        userRepository.save(user);
        return "redirect:/login";
    }

    // --- Authentication Handlers ---

    // Handler for GET /login: Shows the login page
    @GetMapping("/login")
    public String viewLogin() {
        return "page-login";
    }

    // Handler for POST /login: Processes the login attempt and saves user to session
    @PostMapping("/login")
    public String loginUser(@RequestParam String email,
                            @RequestParam String password,
                            Model model,
                            HttpSession session) {

        // Find User by Email (Returns Optional<User>)
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            model.addAttribute("loginError", "Login failed: Email not found.");
            return "page-login";
        }

        User user = userOptional.get();

        // Validate Password (WARNING: Use a PasswordEncoder here in production!)
        if (user.getPassword().equals(password)) {

            // Login Successful! Store the authenticated user's email in the session
            session.setAttribute("userEmail", user.getEmail());

            return "redirect:/";

        } else {
            model.addAttribute("loginError", "Login failed: Incorrect password.");
            return "page-login";
        }
    }

    // NEW: Logout Handler
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }


    // Fetches user details from session and database
    @GetMapping("/profile")
    public String viewProfile(Model model, HttpSession session) {
        Optional<User> userOptional = getCurrentUser(session);

        // Check if the user is logged in
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }

        model.addAttribute("user", userOptional.get());
        return "app-profile";
    }

    // --- Message/Email Handlers ---

    // Fetches the messages for the current user directly using the Repository (Need to update viewInbox() for security)
    @GetMapping("/inbox")
    public String viewInbox() {
        return "email-inbox";
    }


    //Simple handler for the read message page, as conversation logic is not needed
    @GetMapping("/read")
    public String viewRead() {
        return "email-read";
    }

    // Ensures the user is logged in before allowing them to compose
    @GetMapping("/compose")
    public String viewCompose(HttpSession session) {
        if (getCurrentUser(session).isEmpty()) {
            return "redirect:/login";
        }
        return "email-compose";
    }

    // Handler for sending the message, now accepting recipientEmail instead of recipientId
    @PostMapping("/send-message")
    public String sendMessage(
            @RequestParam("recipientEmail") String recipientEmail,
            @RequestParam("content") String content,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Optional<User> senderOptional = getCurrentUser(session);
        if (senderOptional.isEmpty()) {
            return "redirect:/login";
        }
        User sender = senderOptional.get();

        if (content == null || content.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Message content cannot be empty.");
            return "redirect:/compose";
        }

        try {
            // Find Recipient by Email
            User recipient = userRepository.findByEmail(recipientEmail)
                    .orElseThrow(() -> new RuntimeException("Recipient not found with email: " + recipientEmail));

            // Optional check: Prevent sending email to self
            if (sender.getEmail().equalsIgnoreCase(recipientEmail)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cannot send message to yourself.");
                return "redirect:/compose";
            }

            // Create and save the Message
            Message message = new Message(
                    sender,
                    recipient,
                    content,
                    LocalDateTime.now()
            );
            messageRepository.save(message); // Save using the injected repository

            redirectAttributes.addFlashAttribute("successMessage", "Message sent successfully!");
            // Redirect to inbox after successful send
            return "redirect:/inbox";
        } catch (RuntimeException e) {
            // This catches the Recipient not found exception
            redirectAttributes.addFlashAttribute("errorMessage", "Error sending message: " + e.getMessage());
            return "redirect:/compose";
        }
    }


    // --- Personalized Scores Handler ---

    /**
     * Handles /scores and /table to show the personalized score data.
     */
    @GetMapping({"/scores", "/table"})
    public String viewScores(
            @RequestParam(required = false) String subjectName, // Capture the subjectName URL parameter
            Model model,
            HttpSession session) {

        Optional<User> userOptional = getCurrentUser(session);

        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOptional.get();
        List<Score> userScores;

        // 1. Check if a subject filter was provided
        if (subjectName != null && !subjectName.isEmpty()) {
            // Fetch scores ONLY for the selected subject
            userScores = scoreRepository.findByUserAndSubject(user, subjectName);

            // Add the subject name to the model to display it on the page title
            model.addAttribute("currentSubject", subjectName);
        } else {
            // If no filter is provided, fetch all scores (default view)
            userScores = scoreRepository.findByUserOrderBySubjectAsc(user);
            model.addAttribute("currentSubject", "All Subjects");
        }

        // 2. Add Data to Model
        model.addAttribute("user", user);
        model.addAttribute("scores", userScores);

        // 3. The view now shows only the filtered scores
        return "scores";
    }


    //  --- Other Views ---

    // Original /table mapping (overridden above)
    // @GetMapping("/table")
    // public String viewTable() {return "table-basic";}

    @GetMapping("/viewclass")
    public String viewClass() {return "viewclasses";}


    @GetMapping("/forgotpass")
    public String viewPassword() {return "forgot-password";}

    @GetMapping("/helpFeed")
    public String viewHelpFeed() {return "help-feedback";}

    @GetMapping("/newindex")
    public String newindex() {return "index";}

    @GetMapping("/about")
    public String About() {return "about";}


    @GetMapping("/records")
    public String Record() {return "records";}

    @GetMapping("/section")
    public String Section() {return "table-datatable";}

}