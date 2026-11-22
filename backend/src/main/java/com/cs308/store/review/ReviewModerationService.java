package com.cs308.store.review;

import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cs308.store.common.ConflictException;
import com.cs308.store.common.NotFoundException;
import com.cs308.store.common.PageResponse;

/** The product manager's side of reviews: approving or rejecting comments (requirement 5). */
@Service
public class ReviewModerationService {

    static final int MAX_PAGE_SIZE = 50;

    private final ReviewRepository reviews;

    public ReviewModerationService(ReviewRepository reviews) {
        this.reviews = reviews;
    }

    @Transactional(readOnly = true)
    public PageResponse<ModerationItem> pendingComments(int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return PageResponse.from(reviews.findPendingComments(pageable), item -> item);
    }

    /**
     * Approves or rejects a pending comment and records who decided and when. A decided comment
     * cannot be decided again, so the second of two managers gets a 409 instead of overwriting.
     */
    @Transactional
    public Review decide(long reviewId, long moderatorId, ModerationRequest.Decision decision) {
        Review review = reviews.findWithLockById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        if (review.getCommentStatus() == null) {
            throw new ConflictException("This review has no comment to approve or reject");
        }
        if (review.getCommentStatus() != CommentStatus.PENDING) {
            // Locale.ROOT: with a Turkish default locale, toLowerCase() would turn "I" into a dotless "ı".
            throw new ConflictException(
                    "This comment has already been " + review.getCommentStatus().name().toLowerCase(Locale.ROOT));
        }
        review.moderate(decision.toStatus(), moderatorId);
        return review;
    }
}
