package com.cs308.store.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrderItemTest {

    private static OrderItem item(int quantity, String unitPrice) {
        return new OrderItem(1L, 10L, quantity, new BigDecimal(unitPrice), new BigDecimal("20.00"),
                new BigDecimal("40.00"), "Basic Tee", "M / Black");
    }

    @Test
    void lineTotalIsUnitPriceTimesQuantity() {
        assertThat(item(3, "79.90").lineTotal()).isEqualByComparingTo("239.70");
    }

    @Test
    void lineTotalOfOneItemIsItsUnitPrice() {
        assertThat(item(1, "79.90").lineTotal()).isEqualByComparingTo("79.90");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    void quantityMustBeAtLeastOne(int quantity) {
        assertThatThrownBy(() -> item(quantity, "79.90"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
