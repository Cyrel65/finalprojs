package org.example.finalprojs.controller;

import jakarta.servlet.http.HttpSession;
import org.example.finalprojs.model.GradeReport;
import org.example.finalprojs.model.User;
import org.example.finalprojs.service.FeedbackService;
import org.example.finalprojs.service.GradeService;
import org.example.finalprojs.service.UserService;
import org.example.finalprojs.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class MainController {

    private final UserService userService;
    private final GradeService gradeService;
    private final MessageService messageService;


    // Use constructor injection for all dependencies (Best Practice)
    @Autowired
    public MainController(UserService userService, GradeService gradeService, MessageService messageService,  FeedbackService feedbackService) {
        this.userService = userService;
        this.gradeService = gradeService;
        this.messageService = messageService;
    }

    // Private Helper Method for Session User (Uses UserService for lookup)
    private Optional<User> getCurrentUser(HttpSession session) {
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) {
            return Optional.empty();
        }
        return userService.findUserByEmail(userEmail);
    }

    // --- Registration Logic (Delegated) ---

    @GetMapping("/register")
    public String viewRegister(Model model) {
        model.addAttribute("user", new User());
        return "page-register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user) {
        userService.registerUser(user);
        return "redirect:/login";
    }

    // --- Authentication Handlers (Delegated) ---

    @GetMapping("/login")
    public String viewLogin() {
        return "page-login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String email,
                            @RequestParam String password,
                            Model model,
                            HttpSession session) {

        Optional<User> userOptional = userService.authenticate(email, password);

        if (userOptional.isPresent()) {
            session.setAttribute("userEmail", userOptional.get().getEmail());
            return "redirect:/";
        } else {
            model.addAttribute("loginError", "Login failed: Incorrect email or password.");
            return "page-login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }


    // --- Profile Handlers (Delegated) ---

    @GetMapping("/profile")
    public String viewProfile(Model model, HttpSession session) {
        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOptional.get();
        model.addAttribute("user", user);
        model.addAttribute("unreadCount", messageService.getUnreadCount(user));
        return "app-profile";
    }

    @PostMapping("/update/profile-picture")
    public String updateProfilePicture(@RequestParam String profilePictureUrl, HttpSession session, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }

        try {
            User user = userOptional.get();
            userService.updateProfilePicture(user, profilePictureUrl);
            redirectAttributes.addFlashAttribute("success", "Profile picture updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update profile picture: " + e.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam("cpassword") String currentPassword,
            @RequestParam(value = "npassword", required = false) String newPassword,
            @RequestParam(value = "rpassword", required = false) String retypePassword,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOptional.get();

        try {
            // Delegate all complex validation and saving to the UserService
            User updatedUser = userService.updateProfile(user, name, email, currentPassword, newPassword, retypePassword);

            // If email was changed successfully, update session attribute
            session.setAttribute("userEmail", updatedUser.getEmail());

            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        } catch (IllegalArgumentException e) {
            // Catch validation errors (e.g., incorrect password, email already used)
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/profile";
    }


    // --- Personalized Grade Report Handler (Delegated) ---

    @GetMapping({"/scores", "/table"})
    public String viewScores(
            @RequestParam(required = false) String subjectName,
            Model model,
            HttpSession session) {

        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOptional.get();

        // Delegate fetching and calculation logic to the GradeService
        GradeReport report = gradeService.getReportForDisplay(user, subjectName);

        // Add to model
        model.addAttribute("currentSubject", subjectName != null ? subjectName : "All Subjects");
        model.addAttribute("user", user);
        model.addAttribute("report", report);
        model.addAttribute("unreadCount", messageService.getUnreadCount(user));

        return "scores";
    }


    // --- Other Views (Simple redirects remain in the Controller) ---

    @GetMapping("/viewclass")
    public String viewClass() {return "viewclasses";}

    @GetMapping("/forgotpass")
    public String viewPassword() {return "forgot-password";}

    @GetMapping("/newindex")
    public String newindex(Model model, HttpSession session) {
        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isPresent()) {
            User currentUser = userOptional.get();
            model.addAttribute("user", currentUser);
            model.addAttribute("unreadCount", messageService.getUnreadCount(currentUser));
        }
        return "index";
    }

    @GetMapping("/about")
    public String About() {return "about";}


    @GetMapping("/records")
    public String Record() {return "records";}

    @GetMapping("/section")
    public String Section() {return "table-datatable";}

}