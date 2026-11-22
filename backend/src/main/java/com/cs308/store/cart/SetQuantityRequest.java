package com.cs308.store.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

/** Removing a line has its own endpoint, so the quantity is always at least 1 here. */
public record SetQuantityRequest(@Positive @Max(999) int quantity) {
}
