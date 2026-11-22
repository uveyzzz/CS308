package com.cs308.store.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class OrderTest {

    private static Order orderWithCard(String cardLast4) {
        return new Order(7L, new BigDecimal("200.00"), "Tuzla, Istanbul", "Tuzla, Istanbul", cardLast4, "PAY-1");
    }

    @Test
    void newOrderStartsInProcessing() {
        Order order = orderWithCard("4242");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(order.getDeliveredAt()).isNull();
    }

    @Test
    void newOrderHasItsTimestampsSet() {
        Order order = orderWithCard("4242");

        // Hibernate inserts every column, so these must not be left to the database default.
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getUpdatedAt()).isEqualTo(order.getCreatedAt());
    }

    @Test
    void processingOrderCanBeCancelled() {
        assertThat(orderWithCard("4242").isCancellable()).isTrue();
    }

    @Test
    void keepsOnlyTheLastFourDigits() {
        assertThat(orderWithCard("0042").getCardLast4()).isEqualTo("0042");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "424", "42424", "4242424242424242", "42a2", " 4242" })
    void rejectsAnythingButFourDigits(String cardLast4) {
        assertThatThrownBy(() -> orderWithCard(cardLast4))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
