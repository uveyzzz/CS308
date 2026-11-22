package com.cs308.store.review;

import java.time.Instant;

/** One comment in the product manager's approval queue, with the product it is about. */
public record ModerationItem(Long id, Long productId, String productName, String productSlug, int rating,
        String comment, CommentStatus commentStatus, String authorFullName, Instant createdAt) {
}
