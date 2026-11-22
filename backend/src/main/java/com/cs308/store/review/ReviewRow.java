package com.cs308.store.review;

import java.time.Instant;

/** A review together with its author's name, as loaded by {@link ReviewRepository#findRowsByProductId}. */
public record ReviewRow(Long id, int rating, String comment, CommentStatus commentStatus, String authorFullName,
        Instant createdAt) {
}
