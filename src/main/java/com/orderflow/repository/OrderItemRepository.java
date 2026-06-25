package com.orderflow.repository;

import com.orderflow.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    // For analytics: top selling products
    // Returns List of Object[] where element 0 is Product and element 1 is Sum of quantities
    @Query("SELECT oi.product.id, oi.product.name, SUM(oi.quantity) as totalSold " +
           "FROM OrderItem oi " +
           "GROUP BY oi.product.id, oi.product.name " +
           "ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProducts();
}
