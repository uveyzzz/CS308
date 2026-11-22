package com.cs308.store.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A row of the product_variants table: one size/colour combination with its own stock (requirement 3). */
@Entity
@Table(name = "product_variants")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 10)
    private String size;

    @Column(nullable = false, length = 30)
    private String color;

    @Column(nullable = false)
    private int stockQuantity;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    /** JPA needs an empty constructor; application code uses the one below. */
    protected ProductVariant() {
    }

    public ProductVariant(Long productId, String size, String color, int stockQuantity, String sku) {
        this.productId = productId;
        this.size = size;
        this.color = color;
        this.stockQuantity = stockQuantity;
        this.sku = sku;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getSize() {
        return size;
    }

    public String getColor() {
        return color;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public String getSku() {
        return sku;
    }
}
