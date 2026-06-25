package com.orderflow.repository;

import com.orderflow.model.Invoice;
import com.orderflow.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByOrder(Order order);
    Optional<Invoice> findByOrderId(Long orderId);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    boolean existsByInvoiceNumber(String invoiceNumber);
}
