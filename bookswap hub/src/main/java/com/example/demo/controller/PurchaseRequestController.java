package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.BookService;
import com.example.demo.service.PurchaseRequestService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class PurchaseRequestController {

    private final PurchaseRequestService requestService;
    private final BookService bookService;
    private final UserRepository userRepository;

    public PurchaseRequestController(PurchaseRequestService requestService,
            BookService bookService,
            UserRepository userRepository) {
        this.requestService = requestService;
        this.bookService = bookService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ─── Send buy request ─────────────────────────────────────────────────────
    @PostMapping("/books/{id}/request")
    public String sendRequest(@PathVariable Long id,
            @RequestParam(required = false) String message,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            User requester = getCurrentUser(authentication);
            Optional<Book> optBook = bookService.getBookById(id);
            if (optBook.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Book not found.");
                return "redirect:/books";
            }
            requestService.sendRequest(optBook.get(), requester,
                    message != null ? message : "I am interested in buying this book.");
            redirectAttributes.addFlashAttribute("successMessage",
                    "Request sent! The owner will contact you.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/books/" + id;
    }

    // ─── My sent requests ─────────────────────────────────────────────────────
    @GetMapping("/my-requests")
    public String myRequests(Authentication authentication, Model model) {
        User current = getCurrentUser(authentication);
        boolean isLoggedIn = authentication != null
                && authentication.isAuthenticated();
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("requests", requestService.getRequestsByUser(current));
        return "books/my-requests";
    }

    // ─── Owner's incoming requests ────────────────────────────────────────────
    @GetMapping("/owner/requests")
    public String ownerRequests(Authentication authentication, Model model) {
        User current = getCurrentUser(authentication);
        boolean isLoggedIn = authentication != null
                && authentication.isAuthenticated();
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("requests", requestService.getRequestsForOwner(current));
        return "books/owner-requests";
    }

    // ─── Approve ──────────────────────────────────────────────────────────────
    @PostMapping("/requests/{id}/approve")
    public String approveRequest(@PathVariable Long id, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            requestService.approveRequest(id, getCurrentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Request approved!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/owner/requests";
    }

    // ─── Reject ───────────────────────────────────────────────────────────────
    @PostMapping("/requests/{id}/reject")
    public String rejectRequest(@PathVariable Long id, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            requestService.rejectRequest(id, getCurrentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Request rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/owner/requests";
    }
}
