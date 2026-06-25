package com.orderflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class OrderRequest {

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    private String notes;

    @NotEmpty(message = "Order must contain at least one item")
    private List<OrderItemRequest> items;

    // Getters and Setters
    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
}
