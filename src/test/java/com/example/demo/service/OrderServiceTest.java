package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.example.demo.dto.OrderResponse;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.enums.OrderStatus;
import com.example.demo.event.OrderCreatedApplicationEvent;
import com.example.demo.exception.EmptyCartException;
import com.example.demo.exception.InsufficientStockException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        // We'll prepare our test data here.
    }
    @Test
    void placeOrder_shouldCreateOrderSuccessfully() {

        // 1. Create a user
        User user = new User("testuser", "encodedPassword", "USER");
        user.setId(1L);

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(user));

        // 2. Create a product
        Product product = new Product();
        product.setId(10L);
        product.setName("Keyboard");
        product.setPrice(new BigDecimal("1500.00"));
        product.setStockQuantity(20);

        // 3. Create a cart item
        CartItem cartItem = new CartItem();
        cartItem.setId(100L);
        cartItem.setUser(user);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);

        when(cartItemRepository.findByUserId(1L))
                .thenReturn(List.of(cartItem));

        // 4. Mock the stock update
        when(productRepository.decreaseStock(10L, 2))
                .thenReturn(1);

        // 5. Mock saving the order
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(500L);
                    return order;
                });

        // 6. Mock saving order items
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 7. Execute checkout
        OrderResponse response = orderService.placeOrder("testuser");

        // 8. Verify the response
        assertNotNull(response);
        assertEquals(500L, response.getOrderId());
        assertEquals(new BigDecimal("3000.00"), response.getTotalAmount());
        assertEquals("PENDING", response.getStatus());
        assertNotNull(response.getCreatedAt());

        // 9. Verify cart cleanup
        verify(cartItemRepository).deleteAll(List.of(cartItem));

        // 10. Verify that the application event was published
        ArgumentCaptor<Object> eventCaptor =
                ArgumentCaptor.forClass(Object.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertInstanceOf(
                OrderCreatedApplicationEvent.class,
                eventCaptor.getValue());

        OrderCreatedApplicationEvent event =
                (OrderCreatedApplicationEvent) eventCaptor.getValue();

        assertEquals(500L, event.orderId());
    }
    @Test
    void placeOrder_shouldThrowExceptionWhenCartIsEmpty() {

        // Arrange: Create a user
        User user = new User("testuser", "encodedPassword", "USER");
        user.setId(1L);

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(user));

        // Arrange: Return an empty cart
        when(cartItemRepository.findByUserId(1L))
                .thenReturn(List.of());

        // Act & Assert: Checkout should fail
        assertThrows(
                EmptyCartException.class,
                () -> orderService.placeOrder("testuser")
        );

        // Verify: No order should be created
        verify(orderRepository, never()).save(any(Order.class));

        // Verify: Stock should not be decreased
        verify(productRepository, never()).decreaseStock(anyLong(), anyInt());

        // Verify: No application event should be published
        verify(eventPublisher, never()).publishEvent(any());
    }
    @Test
    void placeOrder_shouldThrowExceptionWhenStockIsInsufficient() {

        // Arrange: Create a user
        User user = new User("testuser", "encodedPassword", "USER");
        user.setId(1L);

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(user));

        // Arrange: Create a product
        Product product = new Product();
        product.setId(10L);
        product.setName("Keyboard");
        product.setPrice(new BigDecimal("1500.00"));
        product.setStockQuantity(1);

        // Arrange: User requests 2 keyboards
        CartItem cartItem = new CartItem();
        cartItem.setId(100L);
        cartItem.setUser(user);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);

        when(cartItemRepository.findByUserId(1L))
                .thenReturn(List.of(cartItem));

        // Simulate insufficient stock
        when(productRepository.decreaseStock(10L, 2))
                .thenReturn(0);

        // Act & Assert
        assertThrows(
                InsufficientStockException.class,
                () -> orderService.placeOrder("testuser")
        );

        // Verify that no order was saved
        verify(orderRepository, never()).save(any(Order.class));

        // Verify that the cart was not cleared
        verify(cartItemRepository, never()).deleteAll(anyList());

        // Verify that no event was published
        verify(eventPublisher, never()).publishEvent(any());
    }
    @Test
    void placeOrder_shouldCalculateTotalAmountCorrectly() {

        // Arrange: Create user
        User user = new User("testuser", "encodedPassword", "USER");
        user.setId(1L);

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(user));

        // Arrange: Create keyboard
        Product keyboard = new Product();
        keyboard.setId(10L);
        keyboard.setName("Keyboard");
        keyboard.setPrice(new BigDecimal("1500.00"));
        keyboard.setStockQuantity(10);

        // Arrange: Create mouse
        Product mouse = new Product();
        mouse.setId(20L);
        mouse.setName("Mouse");
        mouse.setPrice(new BigDecimal("500.00"));
        mouse.setStockQuantity(10);

        // Arrange: Add 2 keyboards
        CartItem keyboardItem = new CartItem();
        keyboardItem.setId(101L);
        keyboardItem.setUser(user);
        keyboardItem.setProduct(keyboard);
        keyboardItem.setQuantity(2);

        // Arrange: Add 3 mice
        CartItem mouseItem = new CartItem();
        mouseItem.setId(102L);
        mouseItem.setUser(user);
        mouseItem.setProduct(mouse);
        mouseItem.setQuantity(3);

        when(cartItemRepository.findByUserId(1L))
                .thenReturn(List.of(keyboardItem, mouseItem));

        // Simulate successful stock updates
        when(productRepository.decreaseStock(10L, 2)).thenReturn(1);
        when(productRepository.decreaseStock(20L, 3)).thenReturn(1);

        // Simulate saving the order
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(500L);
                    return order;
                });

        // Simulate saving order items
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Place order
        OrderResponse response = orderService.placeOrder("testuser");

        // Assert: Verify total amount
        assertEquals(
                new BigDecimal("4500.00"),
                response.getTotalAmount()
        );

        // Verify: Both order items were saved
        verify(orderItemRepository, times(2)).save(any(OrderItem.class));

        // Verify: The order was saved twice
        // First to generate its ID, then to update its total.
        verify(orderRepository, times(2)).save(any(Order.class));
    }
    @Test
    void placeOrder_shouldSaveCorrectOrderItemDetails() {

        // Arrange: Create user
        User user = new User("testuser", "encodedPassword", "USER");
        user.setId(1L);

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(user));

        // Arrange: Create product
        Product product = new Product();
        product.setId(10L);
        product.setName("Keyboard");
        product.setPrice(new BigDecimal("1500.00"));
        product.setStockQuantity(10);

        // Arrange: Create cart item
        CartItem cartItem = new CartItem();
        cartItem.setId(101L);
        cartItem.setUser(user);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);

        when(cartItemRepository.findByUserId(1L))
                .thenReturn(List.of(cartItem));

        when(productRepository.decreaseStock(10L, 2))
                .thenReturn(1);

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(500L);
                    return order;
                });

        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse response = orderService.placeOrder("testuser");

        // Capture the OrderItem passed to the repository
        ArgumentCaptor<OrderItem> captor =
                ArgumentCaptor.forClass(OrderItem.class);

        verify(orderItemRepository).save(captor.capture());

        OrderItem savedItem = captor.getValue();

        // Assert
        assertEquals(10L, savedItem.getProduct().getId());
        assertEquals(2, savedItem.getQuantity());
        assertEquals(new BigDecimal("1500.00"), savedItem.getPrice());
        assertEquals(500L, response.getOrderId());
    }
    @Test
    void getOrderHistory_shouldReturnUsersOrders() {

        // Arrange: Create user
        User user = new User("testuser", "encodedPassword", "USER");
        user.setId(1L);

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(user));

        // Arrange: Create first order
        Order order1 = new Order();
        order1.setId(101L);
        order1.setUser(user);
        order1.setTotalAmount(new BigDecimal("3000.00"));
        order1.setStatus(OrderStatus.PENDING);
        order1.setCreatedAt(
                LocalDateTime.of(2026, 10, 20, 10, 0));

        // Arrange: Create second order
        Order order2 = new Order();
        order2.setId(102L);
        order2.setUser(user);
        order2.setTotalAmount(new BigDecimal("4500.00"));
        order2.setStatus(OrderStatus.PENDING);
        order2.setCreatedAt(
                LocalDateTime.of(2026, 10, 21, 10, 0));

        // The repository returns orders newest first
        when(orderRepository.findByUser_IdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(order2, order1));

        // Act
        List<OrderResponse> responses =
                orderService.getOrderHistory("testuser");

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());

        assertEquals(102L, responses.get(0).getOrderId());
        assertEquals(
                new BigDecimal("4500.00"),
                responses.get(0).getTotalAmount());

        assertEquals(101L, responses.get(1).getOrderId());
        assertEquals(
                new BigDecimal("3000.00"),
                responses.get(1).getTotalAmount());

        // Verify repository interaction
        verify(orderRepository)
                .findByUser_IdOrderByCreatedAtDesc(1L);
    }
    @Test
    void getOrderHistory_shouldThrowExceptionWhenUserDoesNotExist() {

        when(userRepository.findByUsername("unknown"))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> orderService.getOrderHistory("unknown")
        );

        verify(orderRepository, never())
                .findByUser_IdOrderByCreatedAtDesc(anyLong());
    }
    @Test
    void placeOrder_shouldThrowExceptionWhenUserDoesNotExist() {

        // Arrange
        when(userRepository.findByUsername("unknown"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                UserNotFoundException.class,
                () -> orderService.placeOrder("unknown")
        );

        // Verify checkout stopped immediately
        verify(cartItemRepository, never()).findByUserId(anyLong());
        verify(orderRepository, never()).save(any(Order.class));
        verify(productRepository, never()).decreaseStock(anyLong(), anyInt());
        verify(cartItemRepository, never()).deleteAll(anyList());
        verify(eventPublisher, never()).publishEvent(any());
    }
    @Test
    void placeOrder_shouldDecreaseStockForAllProducts() {

        // Arrange: Create user
        User user = new User("testuser", "encodedPassword", "USER");
        user.setId(1L);

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(user));

        // Create keyboard
        Product keyboard = new Product();
        keyboard.setId(10L);
        keyboard.setName("Keyboard");
        keyboard.setPrice(new BigDecimal("1500.00"));
        keyboard.setStockQuantity(10);

        // Create mouse
        Product mouse = new Product();
        mouse.setId(20L);
        mouse.setName("Mouse");
        mouse.setPrice(new BigDecimal("500.00"));
        mouse.setStockQuantity(15);

        // Create keyboard cart item
        CartItem keyboardItem = new CartItem();
        keyboardItem.setUser(user);
        keyboardItem.setProduct(keyboard);
        keyboardItem.setQuantity(2);

        // Create mouse cart item
        CartItem mouseItem = new CartItem();
        mouseItem.setUser(user);
        mouseItem.setProduct(mouse);
        mouseItem.setQuantity(3);

        when(cartItemRepository.findByUserId(1L))
                .thenReturn(List.of(keyboardItem, mouseItem));

        // Simulate successful stock updates
        when(productRepository.decreaseStock(10L, 2)).thenReturn(1);
        when(productRepository.decreaseStock(20L, 3)).thenReturn(1);

        // Mock order saving
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(500L);
                    return order;
                });

        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        orderService.placeOrder("testuser");

        // Assert: Verify both stock updates
        verify(productRepository).decreaseStock(10L, 2);
        verify(productRepository).decreaseStock(20L, 3);

        // Verify that stock deduction was attempted exactly twice
        verify(productRepository, times(2))
                .decreaseStock(anyLong(), anyInt());
    }
}
