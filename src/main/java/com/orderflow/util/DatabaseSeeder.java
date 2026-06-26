package com.orderflow.util;

import com.orderflow.model.Product;
import com.orderflow.model.User;
import com.orderflow.repository.ProductRepository;
import com.orderflow.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(UserRepository userRepository,
                          ProductRepository productRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        seedUsers();
        seedProducts();
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            // Admin User
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@admin.orderflow.com");
            admin.setPasswordHash(passwordEncoder.encode("AdminPassword@123"));
            admin.setRole(User.Role.ADMIN);
            admin.setPhone("+919876543210");
            userRepository.save(admin);

            // Staff User
            User staff = new User();
            staff.setUsername("staff");
            staff.setEmail("staff@staff.orderflow.com");
            staff.setPasswordHash(passwordEncoder.encode("StaffPassword@123"));
            staff.setRole(User.Role.STAFF);
            staff.setPhone("+919876543211");
            userRepository.save(staff);

            // Customer User
            User customer = new User();
            customer.setUsername("customer");
            customer.setEmail("customer@orderflow.com");
            customer.setPasswordHash(passwordEncoder.encode("CustomerPassword@123"));
            customer.setRole(User.Role.CUSTOMER);
            customer.setPhone("+919876543212");
            userRepository.save(customer);

            System.out.println("Seeded database with default users:");
            System.out.println("  Admin: admin@admin.orderflow.com / AdminPassword@123");
            System.out.println("  Staff: staff@staff.orderflow.com / StaffPassword@123");
            System.out.println("  Customer: customer@orderflow.com / CustomerPassword@123");
        }
    }

    private void seedProducts() {
        if (productRepository.count() == 0) {
            Product p1 = new Product();
            p1.setName("Smart Laptop");
            p1.setDescription("High performance development laptop with 32GB RAM and 1TB SSD");
            p1.setPrice(BigDecimal.valueOf(79999.00));
            p1.setStockQty(50);
            p1.setCategory("Electronics");
            productRepository.save(p1);

            Product p2 = new Product();
            p2.setName("Wireless Mouse");
            p2.setDescription("Ergonomic Bluetooth wireless mouse with high precision sensor");
            p2.setPrice(BigDecimal.valueOf(1599.00));
            p2.setStockQty(200);
            p2.setCategory("Electronics");
            productRepository.save(p2);

            Product p3 = new Product();
            p3.setName("Mechanical Keyboard");
            p3.setDescription("RGB backlit mechanical keyboard with blue switches");
            p3.setPrice(BigDecimal.valueOf(4500.00));
            p3.setStockQty(100);
            p3.setCategory("Electronics");
            productRepository.save(p3);

            Product p4 = new Product();
            p4.setName("Running Shoes");
            p4.setDescription("Comfortable and durable lightweight sports running shoes");
            p4.setPrice(BigDecimal.valueOf(2999.00));
            p4.setStockQty(80);
            p4.setCategory("Footwear");
            productRepository.save(p4);

            Product p5 = new Product();
            p5.setName("Coffee Mug");
            p5.setDescription("Ceramic heat-sensitive color-changing coffee mug");
            p5.setPrice(BigDecimal.valueOf(499.00));
            p5.setStockQty(150);
            p5.setCategory("Home & Kitchen");
            productRepository.save(p5);

            Product p6 = new Product();
            p6.setName("Noise Cancelling Headphones");
            p6.setDescription("Over-ear active noise cancelling headphones with 30-hour battery life");
            p6.setPrice(BigDecimal.valueOf(12499.00));
            p6.setStockQty(40);
            p6.setCategory("Electronics");
            productRepository.save(p6);

            Product p7 = new Product();
            p7.setName("Smart Watch");
            p7.setDescription("Fitness tracker smart watch with heart rate monitor and AMOLED display");
            p7.setPrice(BigDecimal.valueOf(5999.00));
            p7.setStockQty(75);
            p7.setCategory("Electronics");
            productRepository.save(p7);

            Product p8 = new Product();
            p8.setName("Gaming Monitor");
            p8.setDescription("27-inch 144Hz 1ms IPS gaming monitor with HDR support");
            p8.setPrice(BigDecimal.valueOf(24999.00));
            p8.setStockQty(25);
            p8.setCategory("Electronics");
            productRepository.save(p8);

            Product p9 = new Product();
            p9.setName("Ergonomic Desk Chair");
            p9.setDescription("High-back ergonomic mesh office chair with adjustable lumbar support");
            p9.setPrice(BigDecimal.valueOf(14500.00));
            p9.setStockQty(15);
            p9.setCategory("Furniture");
            productRepository.save(p9);

            Product p10 = new Product();
            p10.setName("Stainless Steel Bottle");
            p10.setDescription("Vacuum insulated double-walled water bottle, keeps cold for 24 hours");
            p10.setPrice(BigDecimal.valueOf(999.00));
            p10.setStockQty(120);
            p10.setCategory("Home & Kitchen");
            productRepository.save(p10);

            System.out.println("Seeded database with 10 sample products.");
        }
    }
}
