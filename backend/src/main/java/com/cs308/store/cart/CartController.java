package com.cs308.store.cart;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * The logged-in customer's cart. SecurityConfig lets only customers in, and the user always
 * comes from the token, so nobody can read or change another user's cart. Every change
 * answers with the whole cart, so the page never has to ask again.
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public List<CartLine> get(@AuthenticationPrincipal Long userId) {
        return cartService.get(userId);
    }

    /** Adds to the cart; the same variant again raises its quantity. */
    @PostMapping("/items")
    public List<CartLine> add(@AuthenticationPrincipal Long userId, @Valid @RequestBody CartLineRequest line) {
        return cartService.add(userId, line);
    }

    /** Called once after login with the guest cart from the browser. */
    @PostMapping("/merge")
    public List<CartLine> merge(@AuthenticationPrincipal Long userId, @Valid @RequestBody MergeCartRequest request) {
        return cartService.merge(userId, request.items());
    }

    @PutMapping("/items/{variantId}")
    public List<CartLine> setQuantity(@AuthenticationPrincipal Long userId, @PathVariable long variantId,
            @Valid @RequestBody SetQuantityRequest request) {
        return cartService.setQuantity(userId, variantId, request.quantity());
    }

    @DeleteMapping("/items/{variantId}")
    public List<CartLine> remove(@AuthenticationPrincipal Long userId, @PathVariable long variantId) {
        return cartService.remove(userId, variantId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal Long userId) {
        cartService.clear(userId);
    }
}
