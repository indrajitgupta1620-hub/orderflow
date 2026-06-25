package com.orderflow.dto;

import java.math.BigDecimal;

public class DashboardSummaryResponse {
    private long totalOrders;
    private BigDecimal totalRevenue;
    private long pendingOrders;
    private long deliveredOrders;

    public DashboardSummaryResponse() {}

    public DashboardSummaryResponse(long totalOrders, BigDecimal totalRevenue, long pendingOrders, long deliveredOrders) {
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue;
        this.pendingOrders = pendingOrders;
        this.deliveredOrders = deliveredOrders;
    }

    // Getters and Setters
    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public long getDeliveredOrders() {
        return deliveredOrders;
    }

    public void setDeliveredOrders(long deliveredOrders) {
        this.deliveredOrders = deliveredOrders;
    }
}
