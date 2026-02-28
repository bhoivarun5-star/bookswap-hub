package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.BookRepository;
import com.example.demo.repository.PurchaseRequestRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;

    public AdminController(UserRepository userRepository,
            BookRepository bookRepository,
            PurchaseRequestRepository purchaseRequestRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
    }

    // ─── Dashboard ────────────────────────────────────────────────────────────
    @GetMapping({ "", "/panel" })
    public String panel(Model model) {
        List<User> users = userRepository.findAllByRoleNot("ROLE_ADMIN");
        List<Book> books = bookRepository.findAll();
        List<PurchaseRequest> requests = purchaseRequestRepository.findAll();

        long totalUsers  = users.size();
        long totalBooks  = books.size();
        long activeBooks = books.stream().filter(b -> !b.isSold()).count();
        long soldBooks   = books.stream().filter(Book::isSold).count();
        long totalRequests = requests.size();

        // ─── Chart: last 14 day labels ───────────────────────────────────────
        LocalDate today = LocalDate.now();
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MMM dd");
        List<LocalDate> last14 = IntStream.range(0, 14)
                .mapToObj(i -> today.minusDays(13 - i))
                .collect(Collectors.toList());
        List<String> chartDays = last14.stream()
                .map(d -> d.format(labelFmt))
                .collect(Collectors.toList());

        // Registrations per day
        Map<LocalDate, Long> regMap = users.stream()
                .filter(u -> u.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        u -> u.getCreatedAt().toLocalDate(), Collectors.counting()));
        List<Long> chartRegData = last14.stream()
                .map(d -> regMap.getOrDefault(d, 0L))
                .collect(Collectors.toList());

        // Books listed per day
        Map<LocalDate, Long> booksMap = books.stream()
                .filter(b -> b.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        b -> b.getCreatedAt().toLocalDate(), Collectors.counting()));
        List<Long> chartBooksData = last14.stream()
                .map(d -> booksMap.getOrDefault(d, 0L))
                .collect(Collectors.toList());

        // Books by category
        Map<BookCategory, Long> catMap = books.stream()
                .collect(Collectors.groupingBy(Book::getCategory, Collectors.counting()));
        List<String> chartCatLabels = Arrays.stream(BookCategory.values())
                .map(BookCategory::getDisplayName)
                .collect(Collectors.toList());
        List<Long> chartCatData = Arrays.stream(BookCategory.values())
                .map(c -> catMap.getOrDefault(c, 0L))
                .collect(Collectors.toList());

        // Request statuses
        long pendingReqs  = requests.stream().filter(r -> r.getStatus() == RequestStatus.PENDING).count();
        long approvedReqs = requests.stream().filter(r -> r.getStatus() == RequestStatus.APPROVED).count();
        long rejectedReqs = requests.stream().filter(r -> r.getStatus() == RequestStatus.REJECTED).count();

        model.addAttribute("users",       users);
        model.addAttribute("books",       books);
        model.addAttribute("totalUsers",  totalUsers);
        model.addAttribute("totalBooks",  totalBooks);
        model.addAttribute("activeBooks", activeBooks);
        model.addAttribute("soldBooks",   soldBooks);
        model.addAttribute("totalRequests", totalRequests);
        // Chart data
        model.addAttribute("chartDays",      chartDays);
        model.addAttribute("chartRegData",   chartRegData);
        model.addAttribute("chartBooksData", chartBooksData);
        model.addAttribute("chartCatLabels", chartCatLabels);
        model.addAttribute("chartCatData",   chartCatData);
        model.addAttribute("pendingReqs",    pendingReqs);
        model.addAttribute("approvedReqs",   approvedReqs);
        model.addAttribute("rejectedReqs",   rejectedReqs);
        return "admin/panel";
    }

    // ─── Delete book ──────────────────────────────────────────────────────────
    @PostMapping("/books/{id}/delete")
    public String deleteBook(@PathVariable long id, RedirectAttributes ra) {
        try {
            bookRepository.deleteById(id);
            ra.addFlashAttribute("successMessage", "Book deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to delete book: " + e.getMessage());
        }
        return "redirect:/admin/panel";
    }

    // ─── Edit book form ───────────────────────────────────────────────────────
    @GetMapping("/books/{id}/edit")
    public String editBookForm(@PathVariable long id, Model model, RedirectAttributes ra) {
        return bookRepository.findById(id).map(book -> {
            model.addAttribute("book", book);
            model.addAttribute("categories", BookCategory.values());
            model.addAttribute("conditions", BookCondition.values());
            model.addAttribute("isAdmin", true);
            return "admin/edit-book";
        }).orElseGet(() -> {
            ra.addFlashAttribute("errorMessage", "Book not found.");
            return "redirect:/admin/panel";
        });
    }

    // ─── Save edited book ──────────────────────────────────────────────────────
    @PostMapping("/books/{id}/edit")
    public String updateBook(@PathVariable long id,
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
            RedirectAttributes ra) {
        try {
            bookRepository.findById(id).ifPresent(book -> {
                book.setTitle(title);
                book.setAuthor(author);
                book.setDescription(description);
                book.setPrice(price);
                book.setCategory(category);
                book.setCondition(condition);
                if (latitude != null)
                    book.setLatitude(latitude);
                if (longitude != null)
                    book.setLongitude(longitude);
                if (address != null && !address.isBlank())
                    book.setAddress(address);
                if (image != null && !image.isEmpty()) {
                    try {
                        book.setImagePath(saveImage(image));
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
                bookRepository.save(book);
            });
            ra.addFlashAttribute("successMessage", "Book updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to update book: " + e.getMessage());
        }
        return "redirect:/admin/panel";
    }

    // ─── Delete user (and all their books + requests) ─────────────────────────
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable long id, RedirectAttributes ra) {
        try {
            userRepository.findById(id).ifPresent(user -> {
                // Delete purchase requests where this user is the requester
                // (on other owners' books) — cascades to chat messages automatically
                purchaseRequestRepository.findByRequesterOrderByCreatedAtDesc(user)
                        .forEach(req -> purchaseRequestRepository.delete(Objects.requireNonNull(req)));
                // Delete all books owned by this user
                // (cascade in Book → PurchaseRequest → ChatMessage handles the rest)
                bookRepository.findByOwnerOrderByCreatedAtDesc(user)
                        .forEach(book -> bookRepository.delete(Objects.requireNonNull(book)));
                userRepository.delete(Objects.requireNonNull(user));
            });
            ra.addFlashAttribute("successMessage", "User and their books deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to delete user: " + e.getMessage());
        }
        return "redirect:/admin/panel";
    }

    // ─── Helper: save image ───────────────────────────────────────────────────────
    private String saveImage(MultipartFile file) throws Exception {
        java.nio.file.Path uploadPath = java.nio.file.Paths.get("uploads/");
        if (!java.nio.file.Files.exists(uploadPath)) {
            java.nio.file.Files.createDirectories(uploadPath);
        }
        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String filename = java.util.UUID.randomUUID() + ext;
        java.nio.file.Files.copy(file.getInputStream(),
                uploadPath.resolve(filename),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }
}
