package com.example.demo.repository;

import com.example.demo.model.Book;
import com.example.demo.model.User;
import com.example.demo.model.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    boolean existsByUserAndBook(User user, Book book);

    Optional<Wishlist> findByUserAndBook(User user, Book book);

    List<Wishlist> findByUserOrderBySavedAtDesc(User user);

    long countByBook(Book book);
}
