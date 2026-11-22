package com.cs308.store.catalog;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.Formula;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A row of the products table: only what listings show. Details live in {@link ProductDetails},
 * stock lives on {@link ProductVariant}.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Lazy: a listing never needs the category, so it is loaded only when asked for. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountRate;

    /** Calculated by the database (GENERATED column), so Java never writes it. */
    @Column(insertable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal finalPrice;

    @Column(length = 500)
    private String mainImageUrl;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal avgRating;

    @Column(nullable = false)
    private int ratingCount;

    @Column(nullable = false)
    private int salesCount;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Total stock of all variants, computed in the same SELECT as the product. This keeps the
     * listing to one query instead of one extra query per product. Read-only.
     */
    @Formula("(SELECT COALESCE(SUM(v.stock_quantity), 0) FROM product_variants v WHERE v.product_id = id)")
    private int totalStock;

    /** JPA needs an empty constructor; application code uses the one below. */
    protected Product() {
    }

    public Product(Category category, String name, String slug, BigDecimal price, BigDecimal discountRate,
            String mainImageUrl) {
        this.category = category;
        this.name = name;
        this.slug = slug;
        this.price = price;
        this.discountRate = discountRate;
        this.mainImageUrl = mainImageUrl;
        this.avgRating = BigDecimal.ZERO;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public String getMainImageUrl() {
        return mainImageUrl;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public int getSalesCount() {
        return salesCount;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getTotalStock() {
        return totalStock;
    }
}
