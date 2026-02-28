package com.example.demo.controller;

import com.example.demo.model.Notification;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService,
                                  UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ─── Notifications page ────────────────────────────────────────────────────
    @GetMapping("/notifications")
    public String notificationsPage(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);

        List<Notification> notifications = notificationService.getNotificationsForUser(user);

        // Mark all as read when the page is opened
        notificationService.markAllRead(user);

        model.addAttribute("isLoggedIn", true);
        model.addAttribute("notifications", notifications);
        return "notifications";
    }
}
