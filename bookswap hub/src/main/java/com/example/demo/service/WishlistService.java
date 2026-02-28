package com.example.demo.service;

import com.example.demo.model.Book;
import com.example.demo.model.User;
import com.example.demo.model.Wishlist;
import com.example.demo.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;

    public WishlistService(WishlistRepository wishlistRepository) {
        this.wishlistRepository = wishlistRepository;
    }

    // ─── Save a book to wishlist ───────────────────────────────────────────────
    @Transactional
    public void saveBook(User user, Book book) {
        if (!wishlistRepository.existsByUserAndBook(user, book)) {
            Wishlist w = new Wishlist();
            w.setUser(user);
            w.setBook(book);
            wishlistRepository.save(w);
        }
    }

    // ─── Remove a book from wishlist ──────────────────────────────────────────
    @Transactional
    public void removeBook(User user, Book book) {
        wishlistRepository.findByUserAndBook(user, book)
                .ifPresent(wishlistRepository::delete);
    }

    // ─── Toggle: save if not saved, remove if already saved ──────────────────
    @Transactional
    public boolean toggleWishlist(User user, Book book) {
        if (wishlistRepository.existsByUserAndBook(user, book)) {
            removeBook(user, book);
            return false; // removed
        } else {
            saveBook(user, book);
            return true;  // added
        }
    }

    // ─── Check if a book is already wishlisted ────────────────────────────────
    public boolean isWishlisted(User user, Book book) {
        return wishlistRepository.existsByUserAndBook(user, book);
    }

    // ─── Get all saved books for a user ───────────────────────────────────────
    public List<Wishlist> getWishlistForUser(User user) {
        return wishlistRepository.findByUserOrderBySavedAtDesc(user);
    }

    // ─── Count how many users wishlisted a book ───────────────────────────────
    public long getWishlistCount(Book book) {
        return wishlistRepository.countByBook(book);
    }
}
