package com.example.demo.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.CartItemResponse;
import com.example.demo.dto.CartResponse;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.exception.CartItemNotFoundException;
import com.example.demo.exception.InsufficientStockException;
import com.example.demo.exception.ProductNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;

@Service
@Transactional
public class CartItemService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartItemService(
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {

        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public CartItem addToCart(String username, Long productId, Integer quantity) {

        // 1. Find the user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found: " + username));

        // 2. Find the product
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + productId));

        // 3. Check whether the product is already in the cart
        Optional<CartItem> existingItem =
                cartItemRepository.findByUserIdAndProductId(
                        user.getId(), product.getId());

        // 4. Calculate the total requested quantity
        int totalQuantity = existingItem
                .map(item -> item.getQuantity() + quantity)
                .orElse(quantity);

        // 5. Validate available stock
        if (totalQuantity > product.getStockQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock. Available stock: "
                            + product.getStockQuantity()
                            + ", requested quantity: " + totalQuantity);
        }

        // 6. Update an existing cart item
        if (existingItem.isPresent()) {

            CartItem cartItem = existingItem.get();
            cartItem.setQuantity(totalQuantity);

            return cartItemRepository.save(cartItem);
        }

        // 7. Otherwise, create a new cart item
        CartItem cartItem = new CartItem();
        cartItem.setUser(user);
        cartItem.setProduct(product);
        cartItem.setQuantity(quantity);

        return cartItemRepository.save(cartItem);
    }
    public List<CartItem> getCartItems(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                    new UserNotFoundException(
                        "User not found: " + username
                    ));

        return cartItemRepository.findByUserId(user.getId());
    }
    public CartItem updateCartItem(
            String username,
            Long cartItemId,
            Integer quantity) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                    new UserNotFoundException(
                        "User not found: " + username
                    ));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() ->
    new CartItemNotFoundException(
        "Cart item not found with id: " + cartItemId));

        if (!cartItem.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You cannot modify another user's cart item"
            );
        }

        cartItem.setQuantity(quantity);

        return cartItemRepository.save(cartItem);
    }
    public void removeCartItem(String username, Long cartItemId) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                    new UserNotFoundException(
                        "User not found: " + username
                    ));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() ->
                    new CartItemNotFoundException(
                        "Cart item not found with id: " + cartItemId
                    ));

        if (!cartItem.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You cannot remove another user's cart item"
            );
        }

        cartItemRepository.delete(cartItem);
    }
    public CartResponse getCart(String username) {

        // Get the user from the database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found: " + username));

        // Get all cart items belonging to this user
        List<CartItem> cartItems =
                cartItemRepository.findByUserId(user.getId());

        // Convert cart items into response DTOs
        List<CartItemResponse> items = cartItems.stream()
                .map(item -> {
                    CartItemResponse response = new CartItemResponse();

                    response.setId(item.getId());
                    response.setProductId(item.getProduct().getId());
                    response.setProductName(item.getProduct().getName());
                    response.setPrice(item.getProduct().getPrice());
                    response.setQuantity(item.getQuantity());

                    BigDecimal subtotal = item.getProduct().getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));

                    response.setSubtotal(subtotal);

                    return response;
                })
                .collect(Collectors.toList());

        // Calculate the total price of all cart items
        BigDecimal total = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create the final cart response
        CartResponse cartResponse = new CartResponse();
        cartResponse.setItems(items);
        cartResponse.setTotal(total);

        return cartResponse;
    }
}
