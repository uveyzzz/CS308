package com.cs308.store.review;

import jakarta.validation.constraints.NotNull;

/** Body of PATCH /api/admin/products/reviews/{id}: {"decision": "APPROVED"} or {"decision": "REJECTED"}. */
public record ModerationRequest(@NotNull(message = "Decision is required") Decision decision) {

    /** Only these two; a comment cannot be moved back to PENDING. */
    public enum Decision {
        APPROVED,
        REJECTED;

        CommentStatus toStatus() {
            return CommentStatus.valueOf(name());
        }
    }
}
