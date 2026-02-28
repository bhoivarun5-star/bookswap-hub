package com.example.demo.repository;

import com.example.demo.model.ChatMessage;
import com.example.demo.model.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRequestOrderBySentAtAsc(PurchaseRequest request);

    List<ChatMessage> findByRequestAndIdGreaterThanOrderBySentAtAsc(PurchaseRequest request, Long lastId);
}
