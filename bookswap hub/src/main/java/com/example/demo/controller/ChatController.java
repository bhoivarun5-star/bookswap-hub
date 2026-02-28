package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.repository.PurchaseRequestRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ChatController {

    private final ChatMessageRepository chatRepo;
    private final PurchaseRequestRepository requestRepo;
    private final UserRepository userRepo;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public ChatController(ChatMessageRepository chatRepo,
                          PurchaseRequestRepository requestRepo,
                          UserRepository userRepo) {
        this.chatRepo = chatRepo;
        this.requestRepo = requestRepo;
        this.userRepo = userRepo;
    }

    private User getCurrentUser(Authentication auth) {
        return userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ─── Chat Page ────────────────────────────────────────────────────────────
    @GetMapping("/chat/{requestId}")
    public String chatPage(@PathVariable long requestId,
                           Authentication authentication,
                           Model model) {
        User current = getCurrentUser(authentication);

        PurchaseRequest req = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // Only the buyer or the owner may access this chat
        boolean isBuyer = req.getRequester().getId().equals(current.getId());
        boolean isOwner = req.getBook().getOwner().getId().equals(current.getId());
        if (!isBuyer && !isOwner) {
            return "redirect:/books";
        }

        // Must be approved
        if (req.getStatus() != RequestStatus.APPROVED) {
            return "redirect:/my-requests";
        }

        List<ChatMessage> messages = chatRepo.findByRequestOrderBySentAtAsc(req);

        model.addAttribute("isLoggedIn", true);
        model.addAttribute("req", req);
        model.addAttribute("currentUser", current);
        model.addAttribute("messages", messages);
        model.addAttribute("otherUser", isBuyer ? req.getBook().getOwner() : req.getRequester());
        return "chat";
    }

    // ─── Send Message (AJAX POST) ─────────────────────────────────────────────
    @PostMapping("/chat/{requestId}/send")
    @ResponseBody
    public ResponseEntity<?> sendMessage(@PathVariable long requestId,
                                         @RequestParam String content,
                                         Authentication authentication) {
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Empty message"));
        }

        User sender = getCurrentUser(authentication);
        PurchaseRequest req = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        boolean isBuyer = req.getRequester().getId().equals(sender.getId());
        boolean isOwner = req.getBook().getOwner().getId().equals(sender.getId());
        if (!isBuyer && !isOwner) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        ChatMessage msg = new ChatMessage();
        msg.setRequest(req);
        msg.setSender(sender);
        msg.setContent(content.trim());
        chatRepo.save(msg);

        return ResponseEntity.ok(toMap(msg, sender.getUsername()));
    }

    // ─── Poll New Messages (AJAX GET) ─────────────────────────────────────────
    @GetMapping("/chat/{requestId}/messages")
    @ResponseBody
    public ResponseEntity<?> pollMessages(@PathVariable long requestId,
                                          @RequestParam(defaultValue = "0") Long after,
                                          Authentication authentication) {
        User current = getCurrentUser(authentication);
        PurchaseRequest req = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        boolean isBuyer = req.getRequester().getId().equals(current.getId());
        boolean isOwner = req.getBook().getOwner().getId().equals(current.getId());
        if (!isBuyer && !isOwner) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        List<Map<String, Object>> result = chatRepo
                .findByRequestAndIdGreaterThanOrderBySentAtAsc(req, after)
                .stream()
                .map(m -> toMap(m, current.getUsername()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────
    private Map<String, Object> toMap(ChatMessage m, String currentUsername) {
        return Map.of(
                "id", m.getId(),
                "sender", m.getSender().getUsername(),
                "content", m.getContent(),
                "time", m.getSentAt() != null ? m.getSentAt().format(TIME_FMT) : "",
                "date", m.getSentAt() != null ? m.getSentAt().format(DATE_FMT) : "",
                "own", m.getSender().getUsername().equals(currentUsername)
        );
    }
}
