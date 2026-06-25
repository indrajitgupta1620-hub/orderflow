package com.orderflow.dto;

import jakarta.validation.constraints.NotNull;

public class StockAdjustmentRequest {

    @NotNull(message = "Adjustment quantity is required")
    private Integer quantity; // Can represent the new absolute quantity or delta. We'll treat it as new absolute quantity.

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
