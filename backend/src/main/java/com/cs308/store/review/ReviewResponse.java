package com.cs308.store.review;

import java.time.Instant;

/**
 * What the author sees right after posting. commentStatus PENDING tells the frontend to say
 * "your comment will appear after approval".
 */
public record ReviewResponse(Long id, Long productId, int rating, String comment, CommentStatus commentStatus,
        Instant createdAt) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(review.getId(), review.getProductId(), review.getRating(), review.getComment(),
                review.getCommentStatus(), review.getCreatedAt());
    }
}
