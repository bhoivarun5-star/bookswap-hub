package com.example.demo.repository;

import com.example.demo.model.PurchaseRequest;
import com.example.demo.model.SaleRecord;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRecordRepository extends JpaRepository<SaleRecord, Long> {

    boolean existsByRequest(PurchaseRequest request);

    Optional<SaleRecord> findByRequest(PurchaseRequest request);

    List<SaleRecord> findByOwnerOrderByReceivedAtDesc(User owner);

    List<SaleRecord> findAllByOrderByReceivedAtDesc();

    @Query("select coalesce(sum(s.amount), 0) from SaleRecord s")
    BigDecimal getTotalSalesAmount();

    @Query("select coalesce(sum(s.amount), 0) from SaleRecord s where s.owner = :owner")
    BigDecimal getTotalSalesAmountByOwner(User owner);
}
