package com.orderflow.service;

import com.orderflow.dto.DashboardSummaryResponse;
import com.orderflow.dto.OrdersByStatusResponse;
import com.orderflow.dto.TopProductResponse;
import com.orderflow.model.Order;
import com.orderflow.repository.OrderItemRepository;
import com.orderflow.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public AnalyticsService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public DashboardSummaryResponse getDashboardSummary() {
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(Order.Status.PENDING);
        long deliveredOrders = orderRepository.countByStatus(Order.Status.DELIVERED);
        
        BigDecimal totalRevenue = orderRepository.sumTotalAmountByStatusNotCancelled();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        return new DashboardSummaryResponse(totalOrders, totalRevenue, pendingOrders, deliveredOrders);
    }

    public List<TopProductResponse> getTopProducts() {
        List<Object[]> results = orderItemRepository.findTopSellingProducts();
        
        return results.stream()
                .limit(10)
                .map(row -> new TopProductResponse(
                        (Long) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue()
                ))
                .collect(Collectors.toList());
    }

    public List<OrdersByStatusResponse> getOrdersByStatus() {
        List<Object[]> results = orderRepository.countOrdersByStatus();
        
        return results.stream()
                .map(row -> new OrdersByStatusResponse(
                        row[0].toString(),
                        ((Number) row[1]).longValue()
                ))
                .collect(Collectors.toList());
    }
}
