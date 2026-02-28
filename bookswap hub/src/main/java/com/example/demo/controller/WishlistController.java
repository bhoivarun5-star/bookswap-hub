package com.example.demo.controller;

import com.example.demo.model.Book;
import com.example.demo.model.User;
import com.example.demo.model.Wishlist;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.BookService;
import com.example.demo.service.WishlistService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class WishlistController {

    private final WishlistService wishlistService;
    private final BookService bookService;
    private final UserRepository userRepository;

    public WishlistController(WishlistService wishlistService,
                              BookService bookService,
                              UserRepository userRepository) {
        this.wishlistService = wishlistService;
        this.bookService = bookService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ─── My Wishlist page ─────────────────────────────────────────────────────
    @GetMapping("/wishlist")
    public String wishlistPage(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        List<Wishlist> items = wishlistService.getWishlistForUser(user);
        model.addAttribute("isLoggedIn", true);
        model.addAttribute("wishlistItems", items);
        return "wishlist";
    }

    // ─── Toggle save/unsave ───────────────────────────────────────────────────
    @PostMapping("/books/{id}/wishlist")
    public String toggleWishlist(@PathVariable Long id,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(authentication);
        Optional<Book> optBook = bookService.getBookById(id);

        if (optBook.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Book not found.");
            return "redirect:/books";
        }

        Book book = optBook.get();

        // Prevent owner from wishlisting their own book
        if (book.getOwner().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot wishlist your own book.");
            return "redirect:/books/" + id;
        }

        boolean added = wishlistService.toggleWishlist(user, book);
        if (added) {
            redirectAttributes.addFlashAttribute("successMessage", "📌 \"" + book.getTitle() + "\" saved to your wishlist!");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Removed \"" + book.getTitle() + "\" from your wishlist.");
        }
        return "redirect:/books/" + id;
    }
}
