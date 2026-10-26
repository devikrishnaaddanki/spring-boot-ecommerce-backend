package com.example.demo.controller;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AddToCartRequest;
import com.example.demo.dto.CartItemResponse;
import com.example.demo.dto.CartResponse;
import com.example.demo.dto.UpdateCartRequest;
import com.example.demo.entity.CartItem;
import com.example.demo.service.CartItemService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartItemService cartItemService;

    public CartController(CartItemService cartItemService) {
        this.cartItemService = cartItemService;
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartItemResponse addToCart(
            @Valid @RequestBody AddToCartRequest request,
            Authentication authentication) {

        String username = authentication.getName();

        CartItem cartItem = cartItemService.addToCart(
                username,
                request.getProductId(),
                request.getQuantity()
        );

        return convertToResponse(cartItem);
    }
    private CartItemResponse convertToResponse(CartItem cartItem) {

        CartItemResponse response = new CartItemResponse();

        response.setId(cartItem.getId());
        response.setProductId(cartItem.getProduct().getId());
        response.setProductName(cartItem.getProduct().getName());
        response.setPrice(cartItem.getProduct().getPrice());
        response.setQuantity(cartItem.getQuantity());

        BigDecimal subtotal = cartItem.getProduct()
                .getPrice()
                .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        response.setSubtotal(subtotal);

        return response;
    }
    @GetMapping
    public CartResponse getCart(Authentication authentication) {
        String username = authentication.getName();

        return cartItemService.getCart(username);
    }
    @PutMapping("/{cartItemId}")
    public CartItemResponse updateCartItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartRequest request,
            Authentication authentication) {

        String username = authentication.getName();

        CartItem updatedItem = cartItemService.updateCartItem(
                username,
                cartItemId,
                request.getQuantity()
        );

        return convertToResponse(updatedItem);
    }
    @DeleteMapping("/{cartItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCartItem(
            @PathVariable Long cartItemId,
            Authentication authentication) {

        String username = authentication.getName();

        cartItemService.removeCartItem(username, cartItemId);
    }
}
