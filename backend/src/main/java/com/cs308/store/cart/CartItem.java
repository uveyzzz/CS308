package com.cs308.store.cart;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** A row of the cart_items table: how many of one variant a customer has in the cart. */
@Entity
@Table(name = "cart_items")
@IdClass(CartItemId.class)
public class CartItem {

    @Id
    private Long userId;

    @Id
    private Long variantId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, updatable = false)
    private Instant addedAt;

    /** JPA needs an empty constructor; application code uses the one below. */
    protected CartItem() {
    }

    public CartItem(Long userId, Long variantId, int quantity) {
        this.userId = userId;
        this.variantId = variantId;
        this.quantity = quantity;
        this.addedAt = Instant.now();
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public int getQuantity() {
        return quantity;
    }
}
