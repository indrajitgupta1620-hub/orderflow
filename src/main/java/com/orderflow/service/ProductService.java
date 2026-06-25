package com.orderflow.service;

import com.orderflow.dto.ProductRequest;
import com.orderflow.dto.ProductResponse;
import com.orderflow.exception.ResourceNotFoundException;
import com.orderflow.mapper.ProductMapper;
import com.orderflow.model.Product;
import com.orderflow.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<ProductResponse> getAllProducts(String category, Pageable pageable) {
        Page<Product> products;
        if (category != null && !category.trim().isEmpty()) {
            products = productRepository.findByCategoryAndIsActiveTrue(category, pageable);
        } else {
            products = productRepository.findByIsActiveTrue(pageable);
        }
        return products.map(ProductMapper::toResponse);
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .filter(Product::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return ProductMapper.toResponse(product);
    }

    // Direct fetch of the Entity for internal use in OrderService
    public Product getProductEntityById(Long id) {
        return productRepository.findById(id)
                .filter(Product::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = ProductMapper.toEntity(request);
        Product saved = productRepository.save(product);
        return ProductMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        ProductMapper.updateEntity(request, product);
        Product updated = productRepository.save(product);
        return ProductMapper.toResponse(updated);
    }

    @Transactional
    public ProductResponse adjustStock(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        if (quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        product.setStockQty(quantity);
        Product updated = productRepository.save(product);
        return ProductMapper.toResponse(updated);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setIsActive(false);
        productRepository.save(product);
    }
}
