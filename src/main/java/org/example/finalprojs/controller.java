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

