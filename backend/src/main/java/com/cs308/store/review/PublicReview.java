package com.cs308.store.review;

import java.time.Instant;

/**
 * A review as everyone sees it on the product page.
 *
 * @param comment    null unless a product manager approved it
 * @param authorName first name and the initial of the last name ("Ada L."), not the full name
 */
public record PublicReview(Long id, int rating, String comment, String authorName, Instant createdAt) {

    public static PublicReview from(ReviewRow row) {
        String visibleComment = row.commentStatus() == CommentStatus.APPROVED ? row.comment() : null;
        return new PublicReview(row.id(), row.rating(), visibleComment, shortName(row.authorFullName()),
                row.createdAt());
    }

    static String shortName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Customer";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }
}
