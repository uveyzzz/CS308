package com.cs308.store.order;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A row of the orders table (V6): one purchase of one customer. Its lines are OrderItem rows.
 *
 * The entity name is CustomerOrder because ORDER is a reserved word in JPQL queries
 * ("SELECT o FROM Order o" would not parse). The class and the table keep the plain name.
 */
@Entity(name = "CustomerOrder")
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String shippingAddress;

    @Column(nullable = false)
    private String billingAddress;

    /** Only the last 4 digits of the card, never the full number (requirements 15, 17). */
    @Column(name = "card_last4", nullable = false, length = 4)
    private String cardLast4;

    /** The mock bank's transaction id. */
    @Column(nullable = false, length = 64)
    private String paymentRef;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant deliveredAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /** JPA needs an empty constructor; application code uses the one below. */
    protected Order() {
    }

    /**
     * A new order starts in PROCESSING. Hibernate writes every column on insert, so the values
     * the database would default (status, timestamps) are set here too.
     */
    public Order(Long userId, BigDecimal totalAmount, String shippingAddress, String billingAddress,
            String cardLast4, String paymentRef) {
        if (cardLast4 == null || !cardLast4.matches("[0-9]{4}")) {
            throw new IllegalArgumentException("cardLast4 must be exactly 4 digits");
        }
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
        this.billingAddress = billingAddress;
        this.cardLast4 = cardLast4;
        this.paymentRef = paymentRef;
        this.status = OrderStatus.PROCESSING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** Only an order that has not left the store yet can be cancelled (requirement 14). */
    public boolean isCancellable() {
        return status == OrderStatus.PROCESSING;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public String getPaymentRef() {
        return paymentRef;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
