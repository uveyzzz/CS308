package com.cs308.store.cart;

import java.math.BigDecimal;

/** One row of CartItemRepository.findLines. */
public interface CartLineView {

    Long getVariantId();

    Long getProductId();

    String getSlug();

    String getName();

    String getSize();

    String getColor();

    BigDecimal getPrice();

    String getImage();

    Integer getQuantity();

    Integer getMaxStock();

    default CartLine toLine() {
        return new CartLine(getVariantId(), getProductId(), getSlug(), getName(), getSize(), getColor(), getPrice(),
                getImage(), getQuantity(), getMaxStock());
    }
}
