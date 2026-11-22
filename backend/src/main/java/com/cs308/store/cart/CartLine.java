package com.cs308.store.cart;

import java.math.BigDecimal;

/**
 * One cart line as the frontend shows it. The field names match the guest cart in the
 * browser, so the cart page works the same before and after login.
 *
 * @param price    the current unit price after any discount
 * @param maxStock the variant's stock now; the quantity never goes above it
 */
public record CartLine(Long variantId, Long productId, String slug, String name, String size, String color,
        BigDecimal price, String image, int quantity, int maxStock) {
}
