package com.cs308.store.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartLineRequest(@NotNull Long variantId, @Positive @Max(999) int quantity) {
}
