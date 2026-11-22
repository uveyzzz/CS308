package com.cs308.store.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PlaceOrderRequestTest {

    private static final PlaceOrderRequest REQUEST = new PlaceOrderRequest(
            List.of(new OrderLineRequest(10L, 1)), "Tuzla", "Tuzla", "4111111111111234");

    @Test
    void lastFourDigitsAreTheEndOfTheCardNumber() {
        assertThat(REQUEST.cardLast4()).isEqualTo("1234");
    }

    @Test
    void toStringNeverShowsTheCardNumber() {
        assertThat(REQUEST.toString()).doesNotContain("4111111111111234").contains("cardNumber=****");
    }
}
