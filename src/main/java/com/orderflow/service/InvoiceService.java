package com.orderflow.service;

import com.orderflow.model.Invoice;
import com.orderflow.model.Order;
import com.orderflow.model.User;
import com.orderflow.repository.InvoiceRepository;
import com.orderflow.repository.OrderRepository;
import com.orderflow.util.InvoicePdfGenerator;
import com.orderflow.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final AuthService authService;

    public InvoiceService(InvoiceRepository invoiceRepository, OrderRepository orderRepository, AuthService authService) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.authService = authService;
    }

    @Transactional
    public Invoice generateInvoiceForOrder(Order order) {
        if (order.getInvoice() != null) {
            return order.getInvoice();
        }
        
        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setAmount(order.getTotalAmount());
        
        String invoiceNumber = "INV-" + order.getId() + "-" + (System.currentTimeMillis() % 100000);
        invoice.setInvoiceNumber(invoiceNumber);
        
        byte[] pdfContent = InvoicePdfGenerator.generate(order, invoiceNumber);
        invoice.setPdfContent(pdfContent);
        
        Invoice saved = invoiceRepository.save(invoice);
        order.setInvoice(saved);
        return saved;
    }

    @Transactional
    public byte[] getInvoicePdf(Long orderId) {
        Invoice invoice = invoiceRepository.findByOrderId(orderId).orElse(null);
        
        if (invoice == null) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
            
            // Check authorization before generating
            User currentUser = authService.getCurrentUser();
            if (currentUser.getRole() == User.Role.CUSTOMER && !order.getUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You are not authorized to view this invoice");
            }
            
            invoice = generateInvoiceForOrder(order);
        } else {
            // Check authorization on existing invoice
            User currentUser = authService.getCurrentUser();
            if (currentUser.getRole() == User.Role.CUSTOMER && !invoice.getOrder().getUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You are not authorized to view this invoice");
            }
        }
        
        return invoice.getPdfContent();
    }
}
