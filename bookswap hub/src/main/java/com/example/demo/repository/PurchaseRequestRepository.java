package com.example.demo.repository;

import com.example.demo.model.Book;
import com.example.demo.model.PurchaseRequest;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    List<PurchaseRequest> findByBookOrderByCreatedAtDesc(Book book);

    List<PurchaseRequest> findByRequesterOrderByCreatedAtDesc(User requester);

    List<PurchaseRequest> findByBookOwnerOrderByCreatedAtDesc(User owner);

    Optional<PurchaseRequest> findByBookAndRequester(Book book, User requester);

    boolean existsByBookAndRequester(Book book, User requester);
}
