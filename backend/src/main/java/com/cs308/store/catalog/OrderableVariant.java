package com.cs308.store.catalog;

import java.math.BigDecimal;

/**
 * What an order needs to know about one variant at purchase time: the price the customer pays,
 * the cost (for profit/loss), the names to copy into the order and whether the product is on sale.
 * Filled by {@link ProductVariantRepository#findOrderable}.
 */
public interface OrderableVariant {

    Long getVariantId();

    String getProductName();

    String getSize();

    String getColor();

    /** products.final_price: the list price with the current discount applied. */
    BigDecimal getUnitPrice();

    BigDecimal getDiscountRate();

    /** product_costs.cost, or 50% of the list price when the product manager did not set one (requirement 11). */
    BigDecimal getUnitCost();

    Boolean getActive();
}
