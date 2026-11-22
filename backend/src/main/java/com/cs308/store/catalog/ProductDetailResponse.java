package com.cs308.store.catalog;

import java.math.BigDecimal;
import java.util.List;

/**
 * Everything the product detail page shows: the product, its details (requirement 9) and the
 * stock of each size/colour (requirement 3). Detail fields are null if the product has no
 * product_details row yet.
 */
public record ProductDetailResponse(Long id, String slug, String name, BigDecimal price,
        BigDecimal discountedPrice, BigDecimal rating, int ratingCount, List<String> images,
        String description, String model, String serialNumber, String warrantyStatus, String distributor,
        String material, String careInstructions, String fit, List<Variant> variants) {

    public record Variant(Long id, String size, String color, int stock) {

        static Variant from(ProductVariant variant) {
            return new Variant(variant.getId(), variant.getSize(), variant.getColor(), variant.getStockQuantity());
        }
    }

    public static ProductDetailResponse from(Product product, ProductDetails details,
            List<ProductVariant> variants) {
        List<String> images = product.getMainImageUrl() == null ? List.of() : List.of(product.getMainImageUrl());
        boolean hasDetails = details != null;
        return new ProductDetailResponse(product.getId(), product.getSlug(), product.getName(),
                product.getPrice(), product.getFinalPrice(), product.getAvgRating(), product.getRatingCount(),
                images,
                hasDetails ? details.getDescription() : null,
                hasDetails ? details.getModel() : null,
                hasDetails ? details.getSerialNumber() : null,
                hasDetails ? details.getWarrantyStatus() : null,
                hasDetails ? details.getDistributorName() : null,
                hasDetails ? details.getMaterial() : null,
                hasDetails ? details.getCareInstructions() : null,
                hasDetails ? details.getFit() : null,
                variants.stream().map(Variant::from).toList());
    }
}
