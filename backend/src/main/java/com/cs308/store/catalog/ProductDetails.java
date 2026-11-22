package com.cs308.store.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A row of the product_details table: what only the detail page shows (requirement 9).
 * One row per product, with the product's id as its primary key.
 */
@Entity
@Table(name = "product_details")
public class ProductDetails {

    @Id
    private Long productId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false, unique = true, length = 100)
    private String serialNumber;

    @Column(length = 200)
    private String material;

    @Column
    private String careInstructions;

    @Column(length = 50)
    private String fit;

    @Column(nullable = false, length = 100)
    private String warrantyStatus;

    @Column(nullable = false, length = 200)
    private String distributorName;

    @Column(length = 200)
    private String distributorContact;

    /** JPA needs an empty constructor; application code uses the one below. */
    protected ProductDetails() {
    }

    public ProductDetails(Long productId, String description, String model, String serialNumber,
            String warrantyStatus, String distributorName) {
        this.productId = productId;
        this.description = description;
        this.model = model;
        this.serialNumber = serialNumber;
        this.warrantyStatus = warrantyStatus;
        this.distributorName = distributorName;
    }

    public Long getProductId() {
        return productId;
    }

    public String getDescription() {
        return description;
    }

    public String getModel() {
        return model;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getMaterial() {
        return material;
    }

    public String getCareInstructions() {
        return careInstructions;
    }

    public String getFit() {
        return fit;
    }

    public String getWarrantyStatus() {
        return warrantyStatus;
    }

    public String getDistributorName() {
        return distributorName;
    }

    public String getDistributorContact() {
        return distributorContact;
    }
}
