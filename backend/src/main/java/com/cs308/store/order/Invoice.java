package com.cs308.store.order;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A row of the invoices table (V6): at most one invoice per order (requirement 4). */
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false, unique = true, length = 30)
    private String invoiceNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, updatable = false)
    private Instant issuedAt;

    /** Null until the PDF has been emailed to the customer. */
    @Column
    private Instant emailedAt;

    /** JPA needs an empty constructor; application code uses the one below. */
    protected Invoice() {
    }

    public Invoice(Long orderId, String invoiceNumber, BigDecimal totalAmount) {
        this.orderId = orderId;
        this.invoiceNumber = invoiceNumber;
        this.totalAmount = totalAmount;
        this.issuedAt = Instant.now();
    }

    public void markEmailed() {
        this.emailedAt = Instant.now();
    }

    public boolean isEmailed() {
        return emailedAt != null;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getEmailedAt() {
        return emailedAt;
    }
}
