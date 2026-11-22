package com.cs308.store.order;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A row of the order_items table (V6): one variant in one order. Price, cost and names are
 * copies taken at purchase time, so later catalog changes do not change this line.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long variantId;

    @Column(nullable = false)
    private int quantity;

    /** Price paid for one item, discount already applied. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountRate;

    /** Cost of one item, for profit/loss (requirement 11). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false, length = 200)
    private String productName;

    /** For example "M / Black". */
    @Column(nullable = false, length = 50)
    private String variantLabel;

    /** JPA needs an empty constructor; application code uses the one below. */
    protected OrderItem() {
    }

    public OrderItem(Long orderId, Long variantId, int quantity, BigDecimal unitPrice, BigDecimal discountRate,
            BigDecimal unitCost, String productName, String variantLabel) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be at least 1");
        }
        this.orderId = orderId;
        this.variantId = variantId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discountRate = discountRate;
        this.unitCost = unitCost;
        this.productName = productName;
        this.variantLabel = variantLabel;
    }

    /** What this line costs the customer: unit price times quantity. */
    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public String getProductName() {
        return productName;
    }

    public String getVariantLabel() {
        return variantLabel;
    }
}
