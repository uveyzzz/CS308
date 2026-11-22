package com.cs308.store.cart;

import java.io.Serializable;
import java.util.Objects;

/** The primary key of a cart line: one user and one variant. */
public class CartItemId implements Serializable {

    private Long userId;
    private Long variantId;

    /** JPA needs an empty constructor. */
    protected CartItemId() {
    }

    public CartItemId(Long userId, Long variantId) {
        this.userId = userId;
        this.variantId = variantId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CartItemId id && Objects.equals(userId, id.userId)
                && Objects.equals(variantId, id.variantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, variantId);
    }
}
