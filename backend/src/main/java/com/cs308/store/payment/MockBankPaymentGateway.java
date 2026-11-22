package com.cs308.store.payment;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Placeholder bank for SCRUM-26: approves every payment. The Sprint 3 mock-bank item replaces this
 * with real approve/decline rules. Card limits and verification are out of scope (requirement 15).
 * It never stores or logs the card number.
 */
@Component
public class MockBankPaymentGateway implements PaymentGateway {

    @Override
    public String charge(BigDecimal amount, String cardNumber) {
        return "MOCK-" + UUID.randomUUID();
    }
}
