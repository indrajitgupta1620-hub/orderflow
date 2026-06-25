package com.orderflow.controller;

import com.orderflow.dto.OrderRequest;
import com.orderflow.dto.OrderResponse;
import com.orderflow.dto.OrderStatusUpdateRequest;
import com.orderflow.model.Order;
import com.orderflow.service.InvoiceService;
import com.orderflow.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final InvoiceService invoiceService;

    public OrderController(OrderService orderService, InvoiceService invoiceService) {
        this.orderService = orderService;
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse order = orderService.placeOrder(request);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Order.Status orderStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                orderStatus = Order.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }
        }
        
        Page<OrderResponse> orders = orderService.getAllOrders(orderStatus, customerId, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderResponse> orders = orderService.getMyOrders(pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        
        Order.Status status;
        try {
            status = Order.Status.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + request.getStatus());
        }
        
        OrderResponse order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(order);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        OrderResponse order = orderService.cancelOrder(id);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {
        byte[] pdfContent = invoiceService.getInvoicePdf(id);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + id + ".pdf");
        
        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }
}
