package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.BookService;
import com.example.demo.service.PurchaseRequestService;
import com.example.demo.service.WishlistService;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
public class BookController {

    private final BookService bookService;
    private final PurchaseRequestService requestService;
    private final UserRepository userRepository;
    private final WishlistService wishlistService;

    public BookController(BookService bookService,
            PurchaseRequestService requestService,
            UserRepository userRepository,
            WishlistService wishlistService) {
        this.bookService = bookService;
        this.requestService = requestService;
        this.userRepository = userRepository;
        this.wishlistService = wishlistService;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private User getCurrentUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ─── Public book listing ──────────────────────────────────────────────────
    @GetMapping("/books")
    public String listBooks(@RequestParam(required = false) String q,
                            @RequestParam(required = false) BookCategory category,
                            @RequestParam(required = false) BookCondition condition,
                            @RequestParam(required = false) BigDecimal minPrice,
                            @RequestParam(required = false) BigDecimal maxPrice,
                            @RequestParam(required = false, defaultValue = "date_desc") String sort,
                            Authentication authentication, Model model) {
        boolean isLoggedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        List<Book> books = bookService.searchBooks(q, category, condition, minPrice, maxPrice, sort);

        model.addAttribute("books", books);
        model.addAttribute("totalBooks", books.size());
        model.addAttribute("categories", BookCategory.values());
        model.addAttribute("conditions", BookCondition.values());
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("q", q != null ? q : "");
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedCondition", condition);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sort", sort);
        if (isLoggedIn) {
            model.addAttribute("currentUsername", getCurrentUser(authentication).getUsername());
        }

        return "books/list";
    }

    // ─── Book detail ──────────────────────────────────────────────────────────
    @GetMapping("/books/{id}")
    public String bookDetail(@PathVariable Long id, Authentication authentication, Model model) {
        Optional<Book> optBook = bookService.getBookById(id);
        if (optBook.isEmpty())
            return "redirect:/books";

        Book book = optBook.get();
        model.addAttribute("book", book);

        boolean isOwner = false;
        boolean hasRequested = false;
        boolean isLoggedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        if (isLoggedIn) {
            User current = getCurrentUser(authentication);
            isOwner = book.getOwner().getId().equals(current.getId());
            model.addAttribute("currentUsername", current.getUsername());

            if (!isOwner) {
                hasRequested = requestService.getRequestsByUser(current)
                        .stream().anyMatch(r -> r.getBook().getId().equals(id));
            }
        }
        boolean isWishlisted = false;
        if (isLoggedIn && !isOwner) {
            isWishlisted = wishlistService.isWishlisted(getCurrentUser(authentication), book);
        }
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("hasRequested", hasRequested);
        model.addAttribute("isWishlisted", isWishlisted);
        model.addAttribute("isLoggedIn", isLoggedIn);
        return "books/detail";
    }

    // ─── Upload form ──────────────────────────────────────────────────────────
    @GetMapping("/books/new")
    public String uploadForm(Authentication authentication, Model model) {
        boolean isLoggedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("categories", BookCategory.values());
        model.addAttribute("conditions", BookCondition.values());
        return "books/upload";
    }

    @PostMapping("/books/new")
    public String uploadBook(@RequestParam String title,
            @RequestParam String author,
            @RequestParam String description,
            @RequestParam BigDecimal price,
            @RequestParam BookCategory category,
            @RequestParam BookCondition condition,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String address,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            User owner = getCurrentUser(authentication);
            bookService.createBook(title, author, description, price,
                    category, condition, latitude, longitude, address, image, owner);
            redirectAttributes.addFlashAttribute("successMessage", "Book listed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to list book: " + e.getMessage());
        }
        return "redirect:/books";
    }

    // ─── Edit form ────────────────────────────────────────────────────────────
    @GetMapping("/books/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication authentication,
            Model model, RedirectAttributes redirectAttributes) {
        Optional<Book> optBook = bookService.getBookById(id);
        if (optBook.isEmpty())
            return "redirect:/books";

        Book book = optBook.get();
        User current = getCurrentUser(authentication);
        if (!book.getOwner().getId().equals(current.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Not authorized.");
            return "redirect:/books";
        }

        boolean isLoggedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("book", book);
        model.addAttribute("categories", BookCategory.values());
        model.addAttribute("conditions", BookCondition.values());
        return "books/edit";
    }

    @PostMapping("/books/{id}/edit")
    public String updateBook(@PathVariable Long id,
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam String description,
            @RequestParam BigDecimal price,
            @RequestParam BookCategory category,
            @RequestParam BookCondition condition,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String address,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            User current = getCurrentUser(authentication);
            bookService.updateBook(id, title, author, description, price,
                    category, condition, latitude, longitude, address, image, current);
            redirectAttributes.addFlashAttribute("successMessage", "Book updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update: " + e.getMessage());
        }
        return "redirect:/books";
    }

    // ─── Delete ───────────────────────────────────────────────────────────────
    @PostMapping("/books/{id}/delete")
    public String deleteBook(@PathVariable Long id, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            bookService.deleteBook(id, getCurrentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Book deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/books/my-listings";
    }

    // ─── Mark as sold ─────────────────────────────────────────────────────────
    @PostMapping("/books/{id}/sold")
    public String markSold(@PathVariable Long id, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            bookService.markAsSold(id, getCurrentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Marked as sold!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/books/my-listings";
    }

    // ─── My listings ──────────────────────────────────────────────────────────
    @GetMapping("/books/my-listings")
    public String myListings(Authentication authentication, Model model) {
        User current = getCurrentUser(authentication);
        boolean isLoggedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("books", bookService.getBooksByOwner(current));
        model.addAttribute("successMessage", model.asMap().get("successMessage"));
        model.addAttribute("errorMessage", model.asMap().get("errorMessage"));
        return "books/my-listings";
    }
}
