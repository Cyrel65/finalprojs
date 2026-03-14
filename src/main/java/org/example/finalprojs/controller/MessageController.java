package org.example.finalprojs.controller;

import jakarta.servlet.http.HttpSession;
import org.example.finalprojs.model.Message;
import org.example.finalprojs.model.User;
import org.example.finalprojs.model.Teacher;
import org.example.finalprojs.model.TeacherClass;
import org.example.finalprojs.service.MessageService;
import org.example.finalprojs.service.UserService;
import org.example.finalprojs.service.ClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;
    private final ClassService classService;

    @Autowired
    public MessageController(MessageService messageService, UserService userService, ClassService classService) {
        this.messageService = messageService;
        this.userService = userService;
        this.classService = classService;
    }

    private Object getCurrentSender(HttpSession session) {
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null || userEmail.isEmpty()) {
            return null;
        }

        Optional<Teacher> teacherOptional = messageService.findTeacherByEmail(userEmail);
        if (teacherOptional.isPresent()) {
            return teacherOptional.get();
        }

        Optional<User> userOptional = userService.findUserByEmail(userEmail);
        if (userOptional.isPresent()) {
            return userOptional.get();
        }

        return null;
    }

    @GetMapping("/")
    public String viewDashboard(Model model, HttpSession session) {
        Object senderObject = getCurrentSender(session);
        if (senderObject == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", senderObject);
        model.addAttribute("unreadCount", messageService.getUnreadCount(senderObject));
        model.addAttribute("sentCount", messageService.getSentCount(senderObject));
        model.addAttribute("pageTitle", "Dashboard");

        if (senderObject instanceof User) {
            User student = (User) senderObject;
            List<TeacherClass> enrolledClasses = classService.getClassesBySection(student.getSection());
            model.addAttribute("teacherClasses", enrolledClasses);
        } else if (senderObject instanceof Teacher) {
            Teacher teacher = (Teacher) senderObject;
            List<TeacherClass> teachingClasses = classService.getClassesByTeacher(teacher.getId());
            model.addAttribute("teacherClasses", teachingClasses);
        }

        return "index";
    }

    @GetMapping("/inbox")
    public String viewInbox(Model model, HttpSession session) {
        Object senderObject = getCurrentSender(session);
        if (senderObject == null) return "redirect:/login";

        model.addAttribute("user", senderObject);
        model.addAttribute("messages", messageService.getInboxMessages(senderObject));
        model.addAttribute("unreadCount", messageService.getUnreadCount(senderObject));
        model.addAttribute("sentCount", messageService.getSentCount(senderObject));
        model.addAttribute("pageTitle", "Inbox");

        return "email-inbox";
    }

    @GetMapping("/sent")
    public String sentMessages(Model model, HttpSession session) {
        Object senderObject = getCurrentSender(session);
        if (senderObject == null) return "redirect:/login";

        model.addAttribute("user", senderObject);
        model.addAttribute("messages", messageService.getSentMessages(senderObject));
        model.addAttribute("sentCount", messageService.getSentCount(senderObject));
        model.addAttribute("unreadCount", messageService.getUnreadCount(senderObject));
        model.addAttribute("pageTitle", "Sent Messages");

        return "email-inbox";
    }

    @GetMapping("/read")
    public String viewRead(@RequestParam Long messageId, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Object senderObject = getCurrentSender(session);
        if (senderObject == null) return "redirect:/login";

        Optional<Message> messageOptional = messageService.getMessageById(messageId);
        if (messageOptional.isEmpty()) return "redirect:/inbox";

        Message message = messageOptional.get();
        messageService.markMessageAsRead(message, senderObject);

        model.addAttribute("user", senderObject);
        model.addAttribute("message", message);
        model.addAttribute("unreadCount", messageService.getUnreadCount(senderObject));
        model.addAttribute("sentCount", messageService.getSentCount(senderObject));

        return "email-read";
    }

    @GetMapping("/compose")
    public String viewCompose(Model model, HttpSession session) {
        Object senderObject = getCurrentSender(session);
        if (senderObject == null) return "redirect:/login";

        model.addAttribute("user", senderObject);
        model.addAttribute("unreadCount", messageService.getUnreadCount(senderObject));
        model.addAttribute("sentCount", messageService.getSentCount(senderObject));
        model.addAttribute("allUsers", messageService.getAllUsers());

        return "email-compose";
    }

    @PostMapping("/send-message")
    public String sendMessage(@RequestParam String recipientEmail, @RequestParam String subject, @RequestParam String content, HttpSession session, RedirectAttributes redirectAttributes) {
        Object senderObject = getCurrentSender(session);
        if (senderObject == null) return "redirect:/login";

        try {
            messageService.sendMessage(senderObject, recipientEmail, subject, content);
            redirectAttributes.addFlashAttribute("success", "Message sent successfully!");
            return "redirect:/inbox";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/compose";
        }
    }

    @PostMapping("/send-reply")
    public String sendReply(@RequestParam Long originalMessageId, @RequestParam String content, HttpSession session, RedirectAttributes redirectAttributes) {
        Object senderObject = getCurrentSender(session);
        if (senderObject == null) return "redirect:/login";

        try {
            messageService.sendReply(originalMessageId, content, senderObject);
            redirectAttributes.addFlashAttribute("success", "Reply sent!");
            return "redirect:/inbox";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/read?messageId=" + originalMessageId;
        }
    }
}