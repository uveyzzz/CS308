package com.cs308.store.review;

/** Where a review's comment is in moderation. A review without a comment has no status. */
public enum CommentStatus {
    PENDING,
    APPROVED,
    REJECTED
}
