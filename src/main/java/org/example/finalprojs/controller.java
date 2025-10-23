package org.example.finalprojs;

import org.example.finalprojs.model.User;
import org.example.finalprojs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class controller {

    @Autowired
    private UserRepository userRepository;

    // --- Registration Logic ---

    @GetMapping("/register")
    public String viewRegister(Model model) {
        // Provides the empty User object for Thymeleaf to bind the form data to
        model.addAttribute("user", new User());
        return "page-register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user) {

        // This line is correct and should now resolve!
        userRepository.save(user);

        return "redirect:/login";
    }


    // Handler for GET /login: Shows the login page (Keep this as is)
    @GetMapping("/login")
    public String viewLogin() {
        return "page-login";
    }

    // NEW: Handler for POST /login: Processes the login attempt
    @PostMapping("/login")
    public String loginUser(@RequestParam String email,
                            @RequestParam String password,
                            Model model) {

        // 1. Find User by Email
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            // User not found
            model.addAttribute("loginError", "Login failed: Email not found.");
            return "page-login";
        }

        User user = userOptional.get();

        // 2. Validate Password
        // 🚨 IMPORTANT: If you were using security (BCrypt), you'd compare the HASH here.
        // Since you are saving plain text for now, we compare plain text:
        if (user.getPassword().equals(password)) {

            // Login Successful!
            // In a real application, you'd set a session or security context here.

            // Redirect to the index page (or dashboard)
            return "redirect:/";

        } else {
            // Password incorrect
            model.addAttribute("loginError", "Login failed: Incorrect password.");
            return "page-login";
        }
    }


    @GetMapping("/")
    public String viewIndex() {
        return "index";
    }

    @GetMapping("/profile")
    public String viewProfile() {
        return "app-profile";
    }

    @GetMapping("/inbox")
    public String viewInbox() {return "email-inbox";
    }

    @GetMapping("/read")
    public String viewRead() {
        return "email-read";
    }

    @GetMapping("/compose")
    public String viewCompose() {
        return "email-compose"; }

    @GetMapping("/table")
    public String viewTable() {
        return "table-basic"; }

    @GetMapping("/viewclass")
    public String viewClass() {
        return "viewclasses"; }

    @GetMapping("/redeem")
    public String viewWidgets() {
        return "widgets"; }

    @GetMapping("/forgotpass")
    public String viewPassword() {
        return "forgot-password"; }

    @GetMapping("/helpFeed")
    public String viewHelpFeed() {
        return "help-feedback"; }

    @GetMapping("/newindex")
    public String newindex() {
        return "index"; }

    @GetMapping("/about")
    public String About() {
        return "about"; }


    @GetMapping("/section")
    public String section() {
        return "table-datatable"; }

    @GetMapping("/records")
    public String Record() {
        return "records"; }
}

