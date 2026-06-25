package com.orderflow.repository;

import com.orderflow.model.Order;
import com.orderflow.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUser(User user, Pageable pageable);
    Page<Order> findByStatus(Order.Status status, Pageable pageable);
    Page<Order> findByUserId(Long userId, Pageable pageable);
    Page<Order> findByStatusAndUserId(Order.Status status, Long userId, Pageable pageable);
    
    // For dashboard and search filters
    long countByStatus(Order.Status status);
    
    Page<Order> findByPlacedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<Order> findByStatusAndPlacedAtBetween(Order.Status status, LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status <> 'CANCELLED'")
    BigDecimal sumTotalAmountByStatusNotCancelled();

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();
}
