package com.cs308.store.payment;

import java.math.BigDecimal;

/**
 * The bank that takes the customer's money (requirements 4, 15). An interface, so the order code
 * does not depend on which bank is behind it and tests can replace it with a mock.
 */
public interface PaymentGateway {

    /**
     * Charges the card.
     *
     * @return the bank's transaction id, stored as orders.payment_ref
     */
    String charge(BigDecimal amount, String cardNumber);
}
