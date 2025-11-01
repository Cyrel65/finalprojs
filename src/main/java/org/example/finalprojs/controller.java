package org.example.finalprojs;

import org.example.finalprojs.model.Box;
import org.example.finalprojs.model.User;
import org.example.finalprojs.model.Message;
import org.example.finalprojs.repository.BoxRepository;
import org.example.finalprojs.repository.MessageRepository;
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
    private MessageRepository messageRepository; // ADDED Repository

    // Private Helper Method for Session User
    private Optional<User> getCurrentUser(HttpSession session) {
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(userEmail);
    }

    //  Registration Logic

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

    // Handler for GET /login: Shows the login page
    @GetMapping("/login")
    public String viewLogin() {
        return "page-login";
    }

    //  Handler for POST /login: Processes the login attempt and saves user to session
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

        // Validate Password
        if (user.getPassword().equals(password)) {

            // Login Successful! Store the authenticated user's email in the session
            session.setAttribute("userEmail", user.getEmail());

            return "redirect:/";

        } else {
            model.addAttribute("loginError", "Login failed: Incorrect password.");
            return "page-login";
        }
    }


    // Fetches user details from session and database
    @GetMapping("/profile")
    public String viewProfile(Model model, HttpSession session) {
        Optional<User> userOptional = getCurrentUser(session); // REFACTORED to use helper method

        // Check if the user is logged in
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }

        model.addAttribute("user", userOptional.get());
        return "app-profile";
    }

    // --- Message/Email Handlers ---

    // Fetches the messages for the current user directly using the Repository
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


    //  Other Views

    @GetMapping("/table")
    public String viewTable() {return "table-basic";}

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

    @GetMapping("/scores")
    public String scores() {return "scores";}

    @GetMapping("/records")
    public String Record() {return "records";}

    @GetMapping("/section")
    public String Section() {return "table-datatable";}
}
