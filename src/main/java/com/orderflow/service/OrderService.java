package com.orderflow.service;

import com.orderflow.dto.OrderItemRequest;
import com.orderflow.dto.OrderRequest;
import com.orderflow.dto.OrderResponse;
import com.orderflow.exception.InsufficientStockException;
import com.orderflow.exception.InvalidOrderStatusTransitionException;
import com.orderflow.exception.ResourceNotFoundException;
import com.orderflow.mapper.OrderMapper;
import com.orderflow.model.Order;
import com.orderflow.model.OrderItem;
import com.orderflow.model.Product;
import com.orderflow.model.User;
import com.orderflow.repository.OrderRepository;
import com.orderflow.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AuthService authService;
    private final InvoiceService invoiceService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        AuthService authService,
                        InvoiceService invoiceService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.authService = authService;
        this.invoiceService = invoiceService;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        User currentUser = authService.getCurrentUser();
        
        Order order = new Order();
        order.setUser(currentUser);
        order.setShippingAddress(request.getShippingAddress());
        order.setNotes(request.getNotes());
        order.setStatus(Order.Status.PENDING);
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .filter(Product::getIsActive)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId()));
            
            if (product.getStockQty() < itemRequest.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName() + 
                        ". Available: " + product.getStockQty() + ", Requested: " + itemRequest.getQuantity());
            }
            
            // Deduct stock
            product.setStockQty(product.getStockQty() - itemRequest.getQuantity());
            productRepository.save(product);
            
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            
            BigDecimal itemSubtotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            orderItem.setSubtotal(itemSubtotal);
            
            totalAmount = totalAmount.add(itemSubtotal);
            order.addOrderItem(orderItem);
        }
        
        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        
        return OrderMapper.toResponse(savedOrder);
    }

    public Page<OrderResponse> getAllOrders(Order.Status status, Long customerId, Pageable pageable) {
        Page<Order> orders;
        if (status != null && customerId != null) {
            orders = orderRepository.findByStatusAndUserId(status, customerId, pageable);
        } else if (status != null) {
            orders = orderRepository.findByStatus(status, pageable);
        } else if (customerId != null) {
            orders = orderRepository.findByUserId(customerId, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }
        return orders.map(OrderMapper::toResponse);
    }

    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        User currentUser = authService.getCurrentUser();
        Page<Order> orders = orderRepository.findByUser(currentUser, pageable);
        return orders.map(OrderMapper::toResponse);
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        
        User currentUser = authService.getCurrentUser();
        
        // Verify owner or STAFF/ADMIN
        if (currentUser.getRole() == User.Role.CUSTOMER && !order.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to view this order");
        }
        
        return OrderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, Order.Status newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        
        Order.Status currentStatus = order.getStatus();
        
        if (currentStatus == newStatus) {
            return OrderMapper.toResponse(order);
        }
        
        // Validate transition
        boolean valid = false;
        switch (currentStatus) {
            case PENDING:
                valid = (newStatus == Order.Status.CONFIRMED || newStatus == Order.Status.CANCELLED);
                break;
            case CONFIRMED:
                valid = (newStatus == Order.Status.PROCESSING || newStatus == Order.Status.CANCELLED);
                break;
            case PROCESSING:
                valid = (newStatus == Order.Status.SHIPPED);
                break;
            case SHIPPED:
                valid = (newStatus == Order.Status.DELIVERED);
                break;
            case DELIVERED:
            case CANCELLED:
                valid = false;
                break;
        }
        
        if (!valid) {
            throw new InvalidOrderStatusTransitionException("Cannot transition order status from " + currentStatus + " to " + newStatus);
        }
        
        order.setStatus(newStatus);
        
        if (newStatus == Order.Status.CANCELLED) {
            // Restore stock
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                product.setStockQty(product.getStockQty() + item.getQuantity());
                productRepository.save(product);
            }
        } else if (newStatus == Order.Status.CONFIRMED) {
            // Generate invoice
            invoiceService.generateInvoiceForOrder(order);
        }
        
        Order updated = orderRepository.save(order);
        return OrderMapper.toResponse(updated);
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        
        User currentUser = authService.getCurrentUser();
        
        // CUSTOMER can cancel own order if PENDING or CONFIRMED. ADMIN can cancel any PENDING or CONFIRMED order.
        if (currentUser.getRole() == User.Role.CUSTOMER) {
            if (!order.getUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You are not authorized to cancel this order");
            }
            if (order.getStatus() != Order.Status.PENDING && order.getStatus() != Order.Status.CONFIRMED) {
                throw new InvalidOrderStatusTransitionException("You can only cancel PENDING or CONFIRMED orders");
            }
        } else if (currentUser.getRole() == User.Role.STAFF) {
            throw new AccessDeniedException("Staff members are not authorized to cancel orders");
        } else { // ADMIN
            if (order.getStatus() != Order.Status.PENDING && order.getStatus() != Order.Status.CONFIRMED) {
                throw new InvalidOrderStatusTransitionException("Admin can only cancel PENDING or CONFIRMED orders");
            }
        }
        
        return updateOrderStatus(id, Order.Status.CANCELLED);
    }
}
