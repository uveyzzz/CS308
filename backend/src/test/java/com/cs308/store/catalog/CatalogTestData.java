package com.cs308.store.catalog;

import java.math.BigDecimal;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * Builds catalog entities for unit tests. Fields the database fills in (id, final_price,
 * the summed stock) are set by reflection, like MeControllerTest does for the user id.
 */
final class CatalogTestData {

    static final Category TOPS = new Category("Tops", "tops", null);

    private CatalogTestData() {
    }

    static Product product(long id, String slug, String price, String finalPrice, int totalStock) {
        Product product = new Product(TOPS, "Product " + id, slug, new BigDecimal(price), BigDecimal.ZERO,
                "/img/" + slug + ".jpg");
        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "finalPrice", new BigDecimal(finalPrice));
        ReflectionTestUtils.setField(product, "totalStock", totalStock);
        return product;
    }

    static ProductVariant variant(long id, long productId, String size, String color, int stock) {
        ProductVariant variant = new ProductVariant(productId, size, color, stock, "SKU-" + id);
        ReflectionTestUtils.setField(variant, "id", id);
        return variant;
    }
}
