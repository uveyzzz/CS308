package com.cs308.store.catalog;

import java.math.BigDecimal;

/**
 * One product card in a listing. Built only from the products table (plus the summed variant
 * stock), never from product_details, so listings stay light (SCRUM-17).
 *
 * @param discountedPrice the price after the discount; equal to price when there is no discount
 * @param stock           total stock of all variants; 0 means out of stock
 */
public record ProductSummary(Long id, String slug, String name, BigDecimal price, BigDecimal discountedPrice,
        BigDecimal rating, int ratingCount, int stock, String imageUrl) {

    public static ProductSummary from(Product product) {
        return new ProductSummary(product.getId(), product.getSlug(), product.getName(), product.getPrice(),
                product.getFinalPrice(), product.getAvgRating(), product.getRatingCount(),
                product.getTotalStock(), product.getMainImageUrl());
    }
}
