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

    @Autowired
    public MainController(UserService userService, GradeService gradeService, MessageService messageService, FeedbackService feedbackService) {
        this.userService = userService;
        this.gradeService = gradeService;
        this.messageService = messageService;
    }

    // Helper: get current user from session
    private Optional<User> getCurrentUser(HttpSession session) {
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) return Optional.empty();
        return userService.findUserByEmail(userEmail);
    }

    // --- Registration ---

    @GetMapping("/register")
    public String viewRegister(Model model) {
        model.addAttribute("user", new User());
        return "page-register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user, RedirectAttributes redirectAttributes) {
        // Validate that section is selected
        if (user.getSection() == null || user.getSection().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a section!");
            return "redirect:/register";
        }

        // Save user (including section)
        userService.registerUser(user);
        redirectAttributes.addFlashAttribute("success", "Registration successful! You can now log in.");
        return "redirect:/login";
    }

    // --- Login / Logout ---

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

    // --- Profile ---

    @GetMapping("/profile")
    public String viewProfile(Model model, HttpSession session) {
        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) return "redirect:/login";

        User user = userOptional.get();
        model.addAttribute("user", user);
        model.addAttribute("unreadCount", messageService.getUnreadCount(user));
        return "app-profile";
    }

    @PostMapping("/update/profile-picture")
    public String updateProfilePicture(@RequestParam String profilePictureUrl, HttpSession session, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) return "redirect:/login";

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
    public String updateProfile(@RequestParam String name,
                                @RequestParam String email,
                                @RequestParam("cpassword") String currentPassword,
                                @RequestParam(value = "npassword", required = false) String newPassword,
                                @RequestParam(value = "rpassword", required = false) String retypePassword,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) return "redirect:/login";

        User user = userOptional.get();

        try {
            User updatedUser = userService.updateProfile(user, name, email, currentPassword, newPassword, retypePassword);
            session.setAttribute("userEmail", updatedUser.getEmail());
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/profile";
    }

    // --- Grades / Reports ---

    @GetMapping({"/scores", "/table"})
    public String viewScores(@RequestParam(required = false) String subjectName,
                             Model model,
                             HttpSession session) {

        Optional<User> userOptional = getCurrentUser(session);
        if (userOptional.isEmpty()) return "redirect:/login";

        User user = userOptional.get();
        GradeReport report = gradeService.getGradeReport(user, subjectName);

        model.addAttribute("currentSubject", subjectName != null ? subjectName : "All Subjects");
        model.addAttribute("user", user);
        model.addAttribute("report", report);
        model.addAttribute("unreadCount", messageService.getUnreadCount(user));

        return "scores";
    }

    // --- Other Pages ---

    @GetMapping("/viewclass")
    public String viewClass() { return "viewclasses"; }

    @GetMapping("/forgotpass")
    public String viewPassword() { return "forgot-password"; }

    @GetMapping("/newindex")
    public String newindex(Model model, HttpSession session) {
        Optional<User> userOptional = getCurrentUser(session);
        userOptional.ifPresent(user -> {
            model.addAttribute("user", user);
            model.addAttribute("unreadCount", messageService.getUnreadCount(user));
        });
        return "index";
    }

    @GetMapping("/about")
    public String about() { return "about"; }

    @GetMapping("/records")
    public String record() { return "records"; }

    @GetMapping("/section")
    public String section() { return "table-datatable"; }
}
