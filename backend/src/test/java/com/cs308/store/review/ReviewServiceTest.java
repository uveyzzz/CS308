package com.cs308.store.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.cs308.store.catalog.ProductRepository;
import com.cs308.store.common.ConflictException;
import com.cs308.store.common.ForbiddenException;
import com.cs308.store.common.NotFoundException;
import com.cs308.store.common.PageResponse;

class ReviewServiceTest {

    private static final long USER = 7L;
    private static final long PRODUCT = 3L;

    private ReviewRepository reviews;
    private ProductRepository products;
    private ReviewService service;

    @BeforeEach
    void setUp() {
        reviews = mock(ReviewRepository.class);
        products = mock(ProductRepository.class);
        service = new ReviewService(reviews, products);
        given(reviews.save(any(Review.class))).willAnswer(invocation -> invocation.getArgument(0));
    }

    /** The product exists and the user has received it: the happy path. */
    private void deliveredToUser() {
        given(products.lockActiveProduct(PRODUCT)).willReturn(Optional.of(PRODUCT));
        given(reviews.hasDeliveredPurchase(USER, PRODUCT)).willReturn(true);
        given(reviews.existsByUserIdAndProductId(USER, PRODUCT)).willReturn(false);
    }

    @Test
    void commentWaitsForApprovalButTheRatingIsSaved() {
        deliveredToUser();

        Review review = service.create(USER, PRODUCT, new CreateReviewRequest(4, "  Great fit  "));

        assertThat(review.getRating()).isEqualTo(4);
        assertThat(review.getComment()).isEqualTo("Great fit");
        assertThat(review.getCommentStatus()).isEqualTo(CommentStatus.PENDING);
        assertThat(review.isCommentVisible()).isFalse();
    }

    @Test
    void ratingWithoutACommentHasNoCommentStatus() {
        deliveredToUser();

        Review review = service.create(USER, PRODUCT, new CreateReviewRequest(5, "   "));

        assertThat(review.getComment()).isNull();
        assertThat(review.getCommentStatus()).isNull();
    }

    @Test
    void productRatingIsRecalculatedAfterTheReviewIsSaved() {
        deliveredToUser();

        service.create(USER, PRODUCT, new CreateReviewRequest(5, null));

        InOrder order = inOrder(products, reviews);
        order.verify(products).lockActiveProduct(PRODUCT);
        order.verify(reviews).save(any(Review.class));
        order.verify(reviews).refreshProductRating(PRODUCT);
    }

    @Test
    void customerWithoutADeliveredOrderCannotReview() {
        given(products.lockActiveProduct(PRODUCT)).willReturn(Optional.of(PRODUCT));
        given(reviews.hasDeliveredPurchase(USER, PRODUCT)).willReturn(false);

        assertThatThrownBy(() -> service.create(USER, PRODUCT, new CreateReviewRequest(5, "Nice")))
                .isInstanceOf(ForbiddenException.class);
        verify(reviews, never()).save(any());
        verify(reviews, never()).refreshProductRating(any());
    }

    @Test
    void secondReviewOfTheSameProductIsRejected() {
        given(products.lockActiveProduct(PRODUCT)).willReturn(Optional.of(PRODUCT));
        given(reviews.hasDeliveredPurchase(USER, PRODUCT)).willReturn(true);
        given(reviews.existsByUserIdAndProductId(USER, PRODUCT)).willReturn(true);

        assertThatThrownBy(() -> service.create(USER, PRODUCT, new CreateReviewRequest(3, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("You have already reviewed this product");
        verify(reviews, never()).save(any());
    }

    @Test
    void unknownOrInactiveProductIsNotFound() {
        given(products.lockActiveProduct(PRODUCT)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(USER, PRODUCT, new CreateReviewRequest(3, null)))
                .isInstanceOf(NotFoundException.class);
        verify(reviews, never()).hasDeliveredPurchase(any(), any());
    }

    @Test
    void listShowsEveryRatingButOnlyApprovedComments() {
        given(products.existsByIdAndActiveTrue(PRODUCT)).willReturn(true);
        Instant now = Instant.now();
        given(reviews.findRowsByProductId(eq(PRODUCT), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(
                        new ReviewRow(1L, 5, "Lovely", CommentStatus.APPROVED, "Ada Lovelace", now),
                        new ReviewRow(2L, 1, "Spam spam", CommentStatus.PENDING, "Bob Builder", now),
                        new ReviewRow(3L, 2, "Rude words", CommentStatus.REJECTED, "Eve Smith", now),
                        new ReviewRow(4L, 4, null, null, "Cem Yilmaz", now))));

        PageResponse<PublicReview> page = service.listForProduct(PRODUCT, 0, 10);

        assertThat(page.content()).extracting(PublicReview::rating).containsExactly(5, 1, 2, 4);
        assertThat(page.content()).extracting(PublicReview::comment).containsExactly("Lovely", null, null, null);
    }

    @Test
    void listOfAnUnknownProductIsNotFound() {
        given(products.existsByIdAndActiveTrue(PRODUCT)).willReturn(false);

        assertThatThrownBy(() -> service.listForProduct(PRODUCT, 0, 10)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void authorIsShownByFirstNameAndInitial() {
        assertThat(PublicReview.shortName("Ada King Lovelace")).isEqualTo("Ada L.");
        assertThat(PublicReview.shortName("Cher")).isEqualTo("Cher");
        assertThat(PublicReview.shortName("  ")).isEqualTo("Customer");
    }
}
