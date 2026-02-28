package com.example.demo.service;

import com.example.demo.model.Notification;
import com.example.demo.model.User;
import com.example.demo.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // ─── Create a notification for a user ─────────────────────────────────────
    public void sendNotification(User recipient, String message) {
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setMessage(message);
        notificationRepository.save(n);
    }

    // ─── Get all notifications for a user ─────────────────────────────────────
    public List<Notification> getNotificationsForUser(User user) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
    }

    // ─── Count unread notifications ────────────────────────────────────────────
    public long getUnreadCount(User user) {
        return notificationRepository.countByRecipientAndReadFalse(user);
    }

    // ─── Mark all notifications as read for a user ────────────────────────────
    public void markAllRead(User user) {
        List<Notification> all = notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
        all.stream().filter(n -> !n.isRead()).forEach(n -> n.setRead(true));
        notificationRepository.saveAll(all);
    }
}
