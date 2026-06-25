package com.orderflow.repository;

import com.orderflow.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByIsActiveTrue(Pageable pageable);
    Page<Product> findByCategoryAndIsActiveTrue(String category, Pageable pageable);
    Page<Product> findByCategory(String category, Pageable pageable);
}
