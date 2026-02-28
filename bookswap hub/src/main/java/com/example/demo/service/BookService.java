package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.BookRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // ─── List all unsold books ─────────────────────────────────────────────────
    public List<Book> getAllUnsoldBooks() {
        return bookRepository.findBySoldFalseOrderByCreatedAtDesc();
    }

    // ─── List unsold books NOT owned by current user ───────────────────────────
    public List<Book> getListingForUser(User currentUser) {
        return bookRepository.findBySoldFalseAndOwnerNotOrderByCreatedAtDesc(currentUser);
    }

    // ─── Search / filter books ─────────────────────────────────────────────────
    public List<Book> searchBooks(String q, BookCategory category, BookCondition condition,
                                  BigDecimal minPrice, BigDecimal maxPrice, String sort) {
        Specification<Book> spec = (root, query, cb) -> cb.isFalse(root.get("sold"));

        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("author")), pattern)));
        }
        if (category != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }
        if (condition != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("condition"), condition));
        }
        if (minPrice != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }

        Sort sortObj = switch (sort == null ? "date_desc" : sort) {
            case "price_asc"  -> Sort.by(Sort.Direction.ASC,  "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "date_asc"   -> Sort.by(Sort.Direction.ASC,  "createdAt");
            case "distance"   -> Sort.by(Sort.Direction.DESC, "createdAt"); // JS re-sorts by distance client-side
            default           -> Sort.by(Sort.Direction.DESC, "createdAt"); // date_desc
        };

        return bookRepository.findAll(spec, sortObj);
    }

    // ─── Owner's listings ──────────────────────────────────────────────────────
    public List<Book> getBooksByOwner(User owner) {
        return bookRepository.findByOwnerOrderByCreatedAtDesc(owner);
    }

    // ─── Get single book ───────────────────────────────────────────────────────
    public Optional<Book> getBookById(long id) {
        return bookRepository.findById(id);
    }

    // ─── Create book ───────────────────────────────────────────────────────────
    public Book createBook(String title, String author, String description,
            BigDecimal price, BookCategory category, BookCondition condition,
            Double latitude, Double longitude, String address,
            MultipartFile imageFile, User owner) throws IOException {

        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setDescription(description);
        book.setPrice(price);
        book.setCategory(category);
        book.setCondition(condition);
        book.setLatitude(latitude);
        book.setLongitude(longitude);
        book.setAddress(address);
        book.setOwner(owner);

        if (imageFile != null && !imageFile.isEmpty()) {
            book.setImageData(imageFile.getBytes());
            book.setImageType(imageFile.getContentType());
        }

        return bookRepository.save(book);
    }

    // ─── Update book ───────────────────────────────────────────────────────────
    public Book updateBook(long id, String title, String author, String description,
            BigDecimal price, BookCategory category, BookCondition condition,
            Double latitude, Double longitude, String address,
            MultipartFile imageFile, User requestingUser) throws IOException {

        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));

        if (!book.getOwner().getId().equals(requestingUser.getId())) {
            throw new SecurityException("Not authorized to edit this book");
        }

        book.setTitle(title);
        book.setAuthor(author);
        book.setDescription(description);
        book.setPrice(price);
        book.setCategory(category);
        book.setCondition(condition);
        book.setLatitude(latitude);
        book.setLongitude(longitude);
        book.setAddress(address);

        if (imageFile != null && !imageFile.isEmpty()) {
            book.setImageData(imageFile.getBytes());
            book.setImageType(imageFile.getContentType());
        }

        return bookRepository.save(book);
    }

    // ─── Delete book ───────────────────────────────────────────────────────────
    public void deleteBook(long id, User requestingUser) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));

        if (!book.getOwner().getId().equals(requestingUser.getId())) {
            throw new SecurityException("Not authorized to delete this book");
        }
        bookRepository.delete(book);
    }

    // ─── Mark as sold ──────────────────────────────────────────────────────────
    public void markAsSold(long id, User requestingUser) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));

        if (!book.getOwner().getId().equals(requestingUser.getId())) {
            throw new SecurityException("Not authorized");
        }
        book.setSold(true);
        bookRepository.save(book);
    }

    // ─── Haversine distance (km) between two lat/lng points ───────────────────
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
