package com.cs308.store.review;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A row of the reviews table (V5): one customer's rating of one product, with an optional
 * comment. The rating is public right away; the comment only once it is APPROVED.
 */
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int rating;

    @Column
    private String comment;

    /** Null exactly when there is no comment (checked by the database too). */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CommentStatus commentStatus;

    @Column
    private Long moderatedBy;

    @Column
    private Instant moderatedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA needs an empty constructor; application code uses the one below. */
    protected Review() {
    }

    /** @param comment null or blank means a rating without a comment */
    public Review(Long productId, Long userId, int rating, String comment) {
        this.productId = productId;
        this.userId = userId;
        this.rating = rating;
        boolean hasComment = comment != null && !comment.isBlank();
        this.comment = hasComment ? comment.trim() : null;
        this.commentStatus = hasComment ? CommentStatus.PENDING : null;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getUserId() {
        return userId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public CommentStatus getCommentStatus() {
        return commentStatus;
    }

    public Long getModeratedBy() {
        return moderatedBy;
    }

    public Instant getModeratedAt() {
        return moderatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** The comment is shown to everyone only when this is true. */
    public boolean isCommentVisible() {
        return commentStatus == CommentStatus.APPROVED;
    }
}
