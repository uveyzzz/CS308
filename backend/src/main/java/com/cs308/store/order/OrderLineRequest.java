package com.cs308.store.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** One cart line in POST /api/orders: which variant and how many. The price is never taken from the client. */
public record OrderLineRequest(
        @NotNull(message = "Variant is required")
        Long variantId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 99, message = "Quantity must be at most 99")
        Integer quantity) {
}
