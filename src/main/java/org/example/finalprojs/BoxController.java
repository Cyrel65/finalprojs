package org.example.finalprojs;

import org.example.finalprojs.model.Box;
import org.example.finalprojs.model.User;
import org.example.finalprojs.repository.BoxRepository;
import org.example.finalprojs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class BoxController {

    // Inject the new BoxRepository
    @Autowired
    private BoxRepository boxRepository;

    // You'll need UserRepository to fetch the user object to link the box
    @Autowired
    private UserRepository userRepository;

    // Placeholder for the current user's ID until security is implemented
    private static final Long CURRENT_USER_ID = 1L;

    // --- 1. Dashboard GET Mapping (To Display Boxes) ---
    @GetMapping("/")
    public String viewDashboard(Model model) {

        // Fetch ALL boxes from the database (public/global logic)
        List<Box> allBoxes = boxRepository.findAll();

        // Add the list of ALL boxes to the model
        model.addAttribute("boxes", allBoxes);

        // You can optionally add a basic check for logged-in status here,
        // but for now, we just return the view.
        return "index"; // Your dashboard template
    }

    // --- 2. Box Creation POST Mapping ---
    @PostMapping("/createBox")
    public String createNewBox(@RequestParam int points,
                               @RequestParam String typeOfTest,
                               RedirectAttributes redirectAttributes) {

        // Fetch the current (hardcoded) user
        User currentUser = userRepository.findById(CURRENT_USER_ID)
                .orElse(null);

        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "User session error.");
            return "redirect:/login";
        }

        // Create the new Box object
        Box newBox = new Box(points, typeOfTest, currentUser);

        // Save the box to the MySQL database
        boxRepository.save(newBox);

        // Redirect back to the dashboard to show the updated list
        return "redirect:/redeem";
    }
}