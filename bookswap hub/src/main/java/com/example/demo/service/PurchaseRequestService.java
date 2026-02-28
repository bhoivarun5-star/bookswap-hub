package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.PurchaseRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PurchaseRequestService {

    private final PurchaseRequestRepository requestRepository;
    private final NotificationService notificationService;

    public PurchaseRequestService(PurchaseRequestRepository requestRepository,
                                   NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.notificationService = notificationService;
    }

    // ─── Send a buy request ────────────────────────────────────────────────────
    @Transactional
    public PurchaseRequest sendRequest(Book book, User requester, String message) {
        if (book.getOwner().getId().equals(requester.getId())) {
            throw new IllegalArgumentException("You cannot request to buy your own book.");
        }
        if (book.isSold()) {
            throw new IllegalArgumentException("This book is already sold.");
        }
        if (requestRepository.existsByBookAndRequester(book, requester)) {
            throw new IllegalArgumentException("You have already sent a request for this book.");
        }
        PurchaseRequest req = new PurchaseRequest();
        req.setBook(book);
        req.setRequester(requester);
        req.setMessage(message);
        req.setStatus(RequestStatus.PENDING);
        PurchaseRequest saved = requestRepository.save(req);
        // Notify the book owner about the new request
        notificationService.sendNotification(book.getOwner(),
                "📖 New buy request for \""
                + book.getTitle()
                + "\" from " + requester.getUsername() + "!");
        return saved;
    }

    // ─── Get requests sent by a user ──────────────────────────────────────────
    public List<PurchaseRequest> getRequestsByUser(User user) {
        return requestRepository.findByRequesterOrderByCreatedAtDesc(user);
    }

    // ─── Get requests received by a book owner ────────────────────────────────
    public List<PurchaseRequest> getRequestsForOwner(User owner) {
        return requestRepository.findByBookOwnerOrderByCreatedAtDesc(owner);
    }

    // ─── Approve a request ────────────────────────────────────────────────────
    @Transactional
    public void approveRequest(long requestId, User owner) {
        PurchaseRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (!req.getBook().getOwner().getId().equals(owner.getId())) {
            throw new SecurityException("Not authorized");
        }
        req.setStatus(RequestStatus.APPROVED);
        requestRepository.save(req);
        // Notify the buyer
        notificationService.sendNotification(req.getRequester(),
                "✅ Your request for \""
                + req.getBook().getTitle()
                + "\" was approved! You can now chat with the owner.");
    }

    // ─── Reject a request ─────────────────────────────────────────────────────
    @Transactional
    public void rejectRequest(long requestId, User owner) {
        PurchaseRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (!req.getBook().getOwner().getId().equals(owner.getId())) {
            throw new SecurityException("Not authorized");
        }
        req.setStatus(RequestStatus.REJECTED);
        requestRepository.save(req);
        // Notify the buyer
        notificationService.sendNotification(req.getRequester(),
                "❌ Your request for \""
                + req.getBook().getTitle()
                + "\" was declined by the owner.");
    }
}
