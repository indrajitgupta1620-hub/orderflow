package com.orderflow.orderflow;

import com.orderflow.dto.AuthResponse;
import com.orderflow.dto.OrderItemRequest;
import com.orderflow.dto.OrderRequest;
import com.orderflow.dto.OrderResponse;
import com.orderflow.dto.ProductRequest;
import com.orderflow.dto.ProductResponse;
import com.orderflow.dto.RegisterRequest;
import com.orderflow.model.Product;
import com.orderflow.model.User;
import com.orderflow.repository.ProductRepository;
import com.orderflow.repository.UserRepository;
import com.orderflow.security.JwtUtil;
import com.orderflow.service.AuthService;
import com.orderflow.service.OrderService;
import com.orderflow.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderflowServicesTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void testUserRegistrationAndLogin() {
        RegisterRequest register = new RegisterRequest();
        register.setUsername("testuser");
        register.setEmail("testuser@orderflow.com");
        register.setPassword("password123");
        register.setPhone("+911234567890");

        AuthResponse regResponse = authService.register(register);
        assertNotNull(regResponse.getToken());
        assertEquals("testuser", regResponse.getUsername());
        assertEquals("CUSTOMER", regResponse.getRole());

        assertTrue(jwtUtil.validateToken(regResponse.getToken(), "testuser@orderflow.com"));
        assertEquals("CUSTOMER", jwtUtil.extractRole(regResponse.getToken()));
    }

    @Test
    void testProductCreationAndStockAdjustment() {
        ProductRequest req = new ProductRequest();
        req.setName("Test Laptop");
        req.setDescription("Unit test product");
        req.setPrice(BigDecimal.valueOf(50000.00));
        req.setStockQty(10);
        req.setCategory("Electronics");

        ProductResponse response = productService.createProduct(req);
        assertNotNull(response.getId());
        assertEquals("Test Laptop", response.getName());
        assertEquals(10, response.getStockQty());

        ProductResponse adjusted = productService.adjustStock(response.getId(), 15);
        assertEquals(15, adjusted.getStockQty());
    }

    @Test
    void testOrderPlacementAndStockDeduction() {
        User user = new User();
        user.setUsername("ordercustomer");
        user.setEmail("ordercustomer@orderflow.com");
        user.setPasswordHash("hashedpassword");
        user.setRole(User.Role.CUSTOMER);
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername("ordercustomer@orderflow.com");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Product product = new Product();
        product.setName("Order Laptop");
        product.setPrice(BigDecimal.valueOf(40000.00));
        product.setStockQty(5);
        product.setIsActive(true);
        productRepository.save(product);

        OrderRequest orderReq = new OrderRequest();
        orderReq.setShippingAddress("123 Test Street");
        orderReq.setNotes("Leave at door");
        
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(product.getId());
        item.setQuantity(2);
        orderReq.setItems(Collections.singletonList(item));

        OrderResponse orderRes = orderService.placeOrder(orderReq);
        
        assertNotNull(orderRes.getId());
        assertEquals("PENDING", orderRes.getStatus());
        assertEquals(BigDecimal.valueOf(80000.00).setScale(2), orderRes.getTotalAmount().setScale(2));

        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(3, updatedProduct.getStockQty());

        OrderResponse cancelled = orderService.cancelOrder(orderRes.getId());
        assertEquals("CANCELLED", cancelled.getStatus());
        
        Product restoredProduct = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(5, restoredProduct.getStockQty());
    }

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    void testStaffRegistrationAndLoginWithCorrectEmail() {
        RegisterRequest register = new RegisterRequest();
        register.setUsername("teststaff");
        register.setEmail("test@staff.orderflow.com");
        register.setPassword("password123");
        register.setRole("STAFF");

        AuthResponse regResponse = authService.register(register);
        assertNotNull(regResponse.getToken());
        assertEquals("STAFF", regResponse.getRole());

        com.orderflow.dto.LoginRequest login = new com.orderflow.dto.LoginRequest();
        login.setEmail("test@staff.orderflow.com");
        login.setPassword("password123");
        AuthResponse loginResponse = authService.login(login);
        assertNotNull(loginResponse.getToken());
        assertEquals("STAFF", loginResponse.getRole());
    }

    @Test
    void testStaffRegistrationWithIncorrectEmailThrowsException() {
        RegisterRequest register = new RegisterRequest();
        register.setUsername("badstaff");
        register.setEmail("badstaff@orderflow.com");
        register.setPassword("password123");
        register.setRole("STAFF");

        assertThrows(IllegalArgumentException.class, () -> authService.register(register));
    }

    @Test
    void testStaffLoginWithIncorrectEmailThrowsException() {
        User user = new User();
        user.setUsername("hackedstaff");
        user.setEmail("hacker@orderflow.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(User.Role.STAFF);
        userRepository.save(user);

        com.orderflow.dto.LoginRequest login = new com.orderflow.dto.LoginRequest();
        login.setEmail("hacker@orderflow.com");
        login.setPassword("password123");
        assertThrows(IllegalArgumentException.class, () -> authService.login(login));
    }

    @Test
    void testAdminRegistrationAndLoginWithCorrectEmail() {
        RegisterRequest register = new RegisterRequest();
        register.setUsername("testadmin");
        register.setEmail("test@admin.orderflow.com");
        register.setPassword("password123");
        register.setRole("ADMIN");

        AuthResponse regResponse = authService.register(register);
        assertNotNull(regResponse.getToken());
        assertEquals("ADMIN", regResponse.getRole());

        com.orderflow.dto.LoginRequest login = new com.orderflow.dto.LoginRequest();
        login.setEmail("test@admin.orderflow.com");
        login.setPassword("password123");
        AuthResponse loginResponse = authService.login(login);
        assertNotNull(loginResponse.getToken());
        assertEquals("ADMIN", loginResponse.getRole());
    }

    @Test
    void testAdminRegistrationWithIncorrectEmailThrowsException() {
        RegisterRequest register = new RegisterRequest();
        register.setUsername("badadmin");
        register.setEmail("badadmin@orderflow.com");
        register.setPassword("password123");
        register.setRole("ADMIN");

        assertThrows(IllegalArgumentException.class, () -> authService.register(register));
    }

    @Test
    void testAdminLoginWithIncorrectEmailThrowsException() {
        User user = new User();
        user.setUsername("hackedadmin");
        user.setEmail("hackeradmin@orderflow.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(User.Role.ADMIN);
        userRepository.save(user);

        com.orderflow.dto.LoginRequest login = new com.orderflow.dto.LoginRequest();
        login.setEmail("hackeradmin@orderflow.com");
        login.setPassword("password123");
        assertThrows(IllegalArgumentException.class, () -> authService.login(login));
    }

    @Test
    void testCustomerRegistrationWithAnyEmail() {
        RegisterRequest register = new RegisterRequest();
        register.setUsername("anycustomer");
        register.setEmail("customer@anydomain.com");
        register.setPassword("password123");
        register.setRole("CUSTOMER");

        AuthResponse regResponse = authService.register(register);
        assertNotNull(regResponse.getToken());
        assertEquals("CUSTOMER", regResponse.getRole());
    }
}
