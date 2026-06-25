package com.orderflow.dto;

public class OrdersByStatusResponse {
    private String status;
    private long count;

    public OrdersByStatusResponse() {}

    public OrdersByStatusResponse(String status, long count) {
        this.status = status;
        this.count = count;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
