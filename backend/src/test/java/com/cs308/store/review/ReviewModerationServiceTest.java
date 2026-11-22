package com.cs308.store.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cs308.store.common.ConflictException;
import com.cs308.store.common.NotFoundException;

class ReviewModerationServiceTest {

    private static final long REVIEW = 12L;
    private static final long MANAGER = 2L;

    private ReviewRepository reviews;
    private ReviewModerationService service;

    @BeforeEach
    void setUp() {
        reviews = mock(ReviewRepository.class);
        service = new ReviewModerationService(reviews);
    }

    private Review existing(String comment) {
        Review review = new Review(3L, 7L, 4, comment);
        given(reviews.findWithLockById(REVIEW)).willReturn(Optional.of(review));
        return review;
    }

    @Test
    void approvedCommentBecomesVisible() {
        Review review = existing("Great fit");

        service.decide(REVIEW, MANAGER, ModerationRequest.Decision.APPROVED);

        assertThat(review.getCommentStatus()).isEqualTo(CommentStatus.APPROVED);
        assertThat(review.isCommentVisible()).isTrue();
    }

    @Test
    void rejectedCommentStaysHiddenButTheRatingIsKept() {
        Review review = existing("Rude words");

        service.decide(REVIEW, MANAGER, ModerationRequest.Decision.REJECTED);

        assertThat(review.getCommentStatus()).isEqualTo(CommentStatus.REJECTED);
        assertThat(review.isCommentVisible()).isFalse();
        assertThat(review.getRating()).isEqualTo(4);
    }

    @Test
    void decisionRecordsWhoDecidedAndWhen() {
        Review review = existing("Great fit");

        service.decide(REVIEW, MANAGER, ModerationRequest.Decision.APPROVED);

        assertThat(review.getModeratedBy()).isEqualTo(MANAGER);
        assertThat(review.getModeratedAt()).isNotNull();
    }

    @Test
    void alreadyDecidedCommentCannotBeDecidedAgain() {
        Review review = existing("Great fit");
        service.decide(REVIEW, MANAGER, ModerationRequest.Decision.APPROVED);

        assertThatThrownBy(() -> service.decide(REVIEW, 99L, ModerationRequest.Decision.REJECTED))
                .isInstanceOf(ConflictException.class)
                .hasMessage("This comment has already been approved");
        assertThat(review.getCommentStatus()).isEqualTo(CommentStatus.APPROVED);
        assertThat(review.getModeratedBy()).isEqualTo(MANAGER);
    }

    @Test
    void ratingWithoutACommentHasNothingToModerate() {
        existing(null);

        assertThatThrownBy(() -> service.decide(REVIEW, MANAGER, ModerationRequest.Decision.APPROVED))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void unknownReviewIsNotFound() {
        given(reviews.findWithLockById(REVIEW)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.decide(REVIEW, MANAGER, ModerationRequest.Decision.APPROVED))
                .isInstanceOf(NotFoundException.class);
    }
}
