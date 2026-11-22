package com.cs308.store.cart;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The guest cart from the browser, sent once right after login. */
public record MergeCartRequest(@NotNull @Size(max = 100) List<@Valid @NotNull CartLineRequest> items) {
}
