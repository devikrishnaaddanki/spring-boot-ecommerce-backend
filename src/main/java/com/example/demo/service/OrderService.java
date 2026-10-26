package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.OrderResponse;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
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

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }
    public List<CartItem> getCartItemsForOrder(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found: " + username));

        List<CartItem> cartItems =
                cartItemRepository.findByUserId(user.getId());

        if (cartItems.isEmpty()) {
            throw new EmptyCartException(
                    "Cannot place an order with an empty cart");
        }

        return cartItems;
    }
    public Order createOrder(User user, List<CartItem> cartItems) {

        // 1. Create the order
        Order order = new Order();

        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalAmount(BigDecimal.ZERO);

        // 2. Save the order first to generate its ID
        Order savedOrder = orderRepository.save(order);

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 3. Create order items
        for (CartItem cartItem : cartItems) {

            OrderItem orderItem = new OrderItem();

            orderItem.setOrder(savedOrder);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());

            BigDecimal price = cartItem.getProduct().getPrice();

            orderItem.setPrice(price);

            BigDecimal subtotal = price.multiply(
                    BigDecimal.valueOf(cartItem.getQuantity()));

            totalAmount = totalAmount.add(subtotal);

            orderItemRepository.save(orderItem);
        }

        // 4. Update the order with its final total
        savedOrder.setTotalAmount(totalAmount);

        return orderRepository.save(savedOrder);
    }
    private void decreaseProductStock(List<CartItem> cartItems) {

        for (CartItem cartItem : cartItems) {

            int updatedRows = productRepository.decreaseStock(
                    cartItem.getProduct().getId(),
                    cartItem.getQuantity());

            if (updatedRows == 0) {
                throw new InsufficientStockException(
                        "Insufficient stock for product: "
                                + cartItem.getProduct().getName());
            }
        }
    }
    public OrderResponse placeOrder(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found: " + username));

        List<CartItem> cartItems = getCartItemsForOrder(username);

        decreaseProductStock(cartItems);

        Order order = createOrder(user, cartItems);

        cartItemRepository.deleteAll(cartItems);

        // Publish an application event for the completed order
        eventPublisher.publishEvent(
                new OrderCreatedApplicationEvent(order.getId())
        );

        return convertToResponse(order);
    }

public List<OrderResponse> getOrderHistory(String username) {

    User user = userRepository.findByUsername(username)
            .orElseThrow(() ->
                    new UserNotFoundException("User not found: " + username));

    List<Order> orders =
            orderRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());

    return orders.stream()
            .map(this::convertToResponse)
            .toList();
}

    private OrderResponse convertToResponse(Order order) {

        OrderResponse response = new OrderResponse();

        response.setOrderId(order.getId());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus().name());
        response.setCreatedAt(order.getCreatedAt());

        return response;
    }
}
