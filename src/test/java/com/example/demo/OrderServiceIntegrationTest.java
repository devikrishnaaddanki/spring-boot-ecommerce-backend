
package com.example.demo;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.dto.OrderResponse;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.exception.InsufficientStockException;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.OrderService;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;
    private Product keyboard;
    private Product mouse;

    @BeforeEach
    void setUp() {

        // Create a user
        user = new User(
                "rollback_test_user_" + java.util.UUID.randomUUID(),
                "encodedPassword",
                "USER"
        );

        user = userRepository.save(user);

        // Create keyboard with sufficient stock
        keyboard = new Product();
        keyboard.setName("Rollback Test Keyboard");
        keyboard.setDescription("Keyboard for integration testing");
        keyboard.setPrice(new BigDecimal("1500.00"));
        keyboard.setStockQuantity(10);

        keyboard = productRepository.save(keyboard);

        // Create mouse with insufficient stock for the rollback test
        mouse = new Product();
        mouse.setName("Rollback Test Mouse");
        mouse.setDescription("Mouse for integration testing");
        mouse.setPrice(new BigDecimal("500.00"));
        mouse.setStockQuantity(1);

        mouse = productRepository.save(mouse);

        // Add 2 keyboards to the cart
        CartItem keyboardItem = new CartItem();
        keyboardItem.setUser(user);
        keyboardItem.setProduct(keyboard);
        keyboardItem.setQuantity(2);

        cartItemRepository.save(keyboardItem);

        // Add 3 mice to the cart.
        // The rollback test should fail because only 1 mouse is available.
        CartItem mouseItem = new CartItem();
        mouseItem.setUser(user);
        mouseItem.setProduct(mouse);
        mouseItem.setQuantity(3);

        cartItemRepository.save(mouseItem);
    }

    @Test
    void placeOrder_shouldRollbackAllChangesWhenStockIsInsufficient() {

        // Act and assert: placing the order must throw an exception
        assertThrows(
                InsufficientStockException.class,
                () -> orderService.placeOrder(user.getUsername())
        );

        // Clear the persistence context before reading database state
        entityManager.clear();

        // Verify keyboard stock was rolled back
        Product keyboardFromDb = productRepository
                .findById(keyboard.getId())
                .orElseThrow();

        assertEquals(10, keyboardFromDb.getStockQuantity());

        // Verify mouse stock remains unchanged
        Product mouseFromDb = productRepository
                .findById(mouse.getId())
                .orElseThrow();

        assertEquals(1, mouseFromDb.getStockQuantity());

        // Verify no order was saved
        assertEquals(
                0,
                orderRepository
                        .findByUser_IdOrderByCreatedAtDesc(user.getId())
                        .size()
        );

        // Verify both cart items remain
        assertEquals(
                2,
                cartItemRepository.findByUserId(user.getId()).size()
        );
    }

    @Test
    void placeOrder_shouldCreateOrderAndUpdateStockSuccessfully() {

        // Arrange: Change the mouse quantity to 1,
        // matching the available stock for a successful order.
        var cartItems = cartItemRepository.findByUserId(user.getId());

        CartItem mouseCartItem = cartItems.stream()
                .filter(item -> item.getProduct().getId().equals(mouse.getId()))
                .findFirst()
                .orElseThrow();

        mouseCartItem.setQuantity(1);

        cartItemRepository.save(mouseCartItem);

        // Act: Place the order using the actual service
        OrderResponse response = orderService.placeOrder(user.getUsername());

        // Clear the persistence context before verifying database state
        entityManager.clear();

        // Assert 1: An order was created
        assertNotNull(response);
        assertNotNull(response.getOrderId());

        // Assert 2: Verify keyboard stock decreased from 10 to 8
        Product keyboardFromDb = productRepository
                .findById(keyboard.getId())
                .orElseThrow();

        assertEquals(8, keyboardFromDb.getStockQuantity());

        // Assert 3: Verify mouse stock decreased from 1 to 0
        Product mouseFromDb = productRepository
                .findById(mouse.getId())
                .orElseThrow();

        assertEquals(0, mouseFromDb.getStockQuantity());

        // Assert 4: Verify the cart is empty
        assertEquals(
                0,
                cartItemRepository.findByUserId(user.getId()).size()
        );

        // Assert 5: Verify the order exists in the database
        var orders = orderRepository
                .findByUser_IdOrderByCreatedAtDesc(user.getId());

        assertEquals(1, orders.size());

        // Assert 6: Verify the total amount
        // Keyboard: 2 × ₹1500 = ₹3000
        // Mouse:    1 × ₹500  = ₹500
        // Total:                 ₹3500
        assertEquals(
                0,
                new BigDecimal("3500.00")
                        .compareTo(orders.get(0).getTotalAmount())
        );

        // Assert 7: Verify order status
        assertEquals("PENDING", orders.get(0).getStatus().toString());
    }
}