package org.example.finalprojs;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class controller {

    @GetMapping("/")
    public String viewIndex() {
        return "index";
    }

    @GetMapping("/profile")
    public String viewProfile() {
        return "app-profile";
    }

    @GetMapping("/login")
    public String viewLogin() {
        return "page-login";
    }

    @GetMapping("/register")
    public String viewRegister() {
        return "page-register";
    }
}