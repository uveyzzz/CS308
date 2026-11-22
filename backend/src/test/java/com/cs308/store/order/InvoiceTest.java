package com.cs308.store.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class InvoiceTest {

    private static Invoice invoice() {
        return new Invoice(1L, "INV-2026-000001", new BigDecimal("200.00"));
    }

    @Test
    void newInvoiceIsIssuedButNotEmailed() {
        Invoice invoice = invoice();

        assertThat(invoice.getIssuedAt()).isNotNull();
        assertThat(invoice.isEmailed()).isFalse();
    }

    @Test
    void markEmailedRecordsWhenTheEmailWasSent() {
        Invoice invoice = invoice();

        invoice.markEmailed();

        assertThat(invoice.isEmailed()).isTrue();
        assertThat(invoice.getEmailedAt()).isNotNull();
    }
}
