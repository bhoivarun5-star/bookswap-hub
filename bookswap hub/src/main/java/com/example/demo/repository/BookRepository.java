package com.example.demo.repository;

import com.example.demo.model.Book;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    List<Book> findBySoldFalseOrderByCreatedAtDesc();

    List<Book> findByOwnerOrderByCreatedAtDesc(User owner);

    List<Book> findBySoldFalseAndOwnerNotOrderByCreatedAtDesc(User owner);
}
