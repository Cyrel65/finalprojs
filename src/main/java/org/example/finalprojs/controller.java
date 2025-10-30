package org.example.finalprojs;

import org.example.finalprojs.model.Box;
import org.example.finalprojs.model.User;
import org.example.finalprojs.repository.BoxRepository;
import org.example.finalprojs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession; // Use jakarta for modern Spring Boot
import java.util.List;
import java.util.Optional;

@Controller
public class controller {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BoxRepository boxRepository;

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

    // UPDATED: Handler for POST /login: Processes the login attempt and saves user to session
    @PostMapping("/login")
    public String loginUser(@RequestParam String email,
                            @RequestParam String password,
                            Model model,
                            HttpSession session) { // Added HttpSession

        // Find User by Email
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            // User not found
            model.addAttribute("loginError", "Login failed: Email not found.");
            return "page-login";
        }

        User user = userOptional.get();

        // Validate Password
        if (user.getPassword().equals(password)) {

            // Login Successful!
            // Store the authenticated user's email in the session
            session.setAttribute("userEmail", user.getEmail());

            // Redirect to the index page (or dashboard)
            return "redirect:/";

        } else {
            // Password incorrect
            model.addAttribute("loginError", "Login failed: Incorrect password.");
            return "page-login";
        }
    }


    // Fetches user details from session and database
    @GetMapping("/profile")
    public String viewProfile(Model model, HttpSession session) {
        String userEmail = (String) session.getAttribute("userEmail");

        // Check if the user is logged in
        if (userEmail == null) {
            // If not logged in, redirect to login page
            return "redirect:/login";
        }

        // Fetch the complete User object from the database using the stored email
        Optional<User> userOptional = userRepository.findByEmail(userEmail);

        if (userOptional.isPresent()) {
            // Add the User object to the Model for Thymeleaf to use
            model.addAttribute("user", userOptional.get());
            return "app-profile";
        } else {
            // Should not happen if registration is correct, but handles case where session data is stale
            session.invalidate(); // Clear invalid session
            return "redirect:/login";
        }
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
    public String viewWidgets(Model model) {

        // 1. Fetch ALL boxes from the database (just like the dashboard)
        List<Box> allBoxes = boxRepository.findAll();

        // 2. Add the list of ALL boxes to the model, using the variable name 'boxes'
        //    that widgets.html expects.
        model.addAttribute("boxes", allBoxes);

        // 3. Return the template name
        return "widgets";
    }

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


    @GetMapping("/scores")
    public String scores() {
        return "scores"; }

    @GetMapping("/records")
    public String Record() {
        return "records"; }

    @GetMapping("/section")
    public String Section() {
        return "table-datatable"; }
}