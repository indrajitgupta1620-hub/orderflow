package com.orderflow.controller;

import com.orderflow.dto.DashboardSummaryResponse;
import com.orderflow.dto.OrdersByStatusResponse;
import com.orderflow.dto.TopProductResponse;
import com.orderflow.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
        DashboardSummaryResponse summary = analyticsService.getDashboardSummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<TopProductResponse>> getTopProducts() {
        List<TopProductResponse> topProducts = analyticsService.getTopProducts();
        return ResponseEntity.ok(topProducts);
    }

    @GetMapping("/orders-by-status")
    public ResponseEntity<List<OrdersByStatusResponse>> getOrdersByStatus() {
        List<OrdersByStatusResponse> ordersByStatus = analyticsService.getOrdersByStatus();
        return ResponseEntity.ok(ordersByStatus);
    }
}
