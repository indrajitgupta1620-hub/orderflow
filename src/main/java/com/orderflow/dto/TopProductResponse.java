package com.orderflow.dto;

public class TopProductResponse {
    private Long productId;
    private String name;
    private long unitsSold;

    public TopProductResponse() {}

    public TopProductResponse(Long productId, String name, long unitsSold) {
        this.productId = productId;
        this.name = name;
        this.unitsSold = unitsSold;
    }

    // Getters and Setters
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getUnitsSold() {
        return unitsSold;
    }

    public void setUnitsSold(long unitsSold) {
        this.unitsSold = unitsSold;
    }
}
