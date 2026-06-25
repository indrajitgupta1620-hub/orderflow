package com.orderflow.mapper;

import com.orderflow.dto.OrderItemResponse;
import com.orderflow.dto.OrderResponse;
import com.orderflow.model.Order;
import com.orderflow.model.OrderItem;

import java.util.stream.Collectors;

public class OrderMapper {

    public static OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUser().getId());
        response.setUserEmail(order.getUser().getEmail());
        response.setStatus(order.getStatus().name());
        response.setTotalAmount(order.getTotalAmount());
        response.setShippingAddress(order.getShippingAddress());
        response.setNotes(order.getNotes());
        response.setPlacedAt(order.getPlacedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        
        if (order.getOrderItems() != null) {
            response.setOrderItems(order.getOrderItems().stream()
                    .map(OrderMapper::toItemResponse)
                    .collect(Collectors.toList()));
        }

        if (order.getInvoice() != null) {
            response.setInvoiceId(order.getInvoice().getId());
            response.setInvoiceNumber(order.getInvoice().getInvoiceNumber());
        }

        return response;
    }

    public static OrderItemResponse toItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProduct().getId());
        response.setProductName(item.getProduct().getName());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setSubtotal(item.getSubtotal());
        return response;
    }
}
