package com.orderflow.mapper;

import com.orderflow.dto.ProductRequest;
import com.orderflow.dto.ProductResponse;
import com.orderflow.model.Product;

public class ProductMapper {

    public static Product toEntity(ProductRequest request) {
        if (request == null) {
            return null;
        }
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQty(request.getStockQty());
        product.setCategory(request.getCategory());
        return product;
    }

    public static ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setStockQty(product.getStockQty());
        response.setCategory(product.getCategory());
        response.setIsActive(product.getIsActive());
        response.setCreatedAt(product.getCreatedAt());
        return response;
    }

    public static void updateEntity(ProductRequest request, Product product) {
        if (request == null || product == null) {
            return;
        }
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQty(request.getStockQty());
        product.setCategory(request.getCategory());
    }
}
