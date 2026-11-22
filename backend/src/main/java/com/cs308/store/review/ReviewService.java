package com.cs308.store.review;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cs308.store.catalog.ProductRepository;
import com.cs308.store.common.ConflictException;
import com.cs308.store.common.ForbiddenException;
import com.cs308.store.common.NotFoundException;
import com.cs308.store.common.PageResponse;

/** Ratings and comments (requirement 5, SCRUM-32). */
@Service
public class ReviewService {

    static final int MAX_PAGE_SIZE = 50;

    private final ReviewRepository reviews;
    private final ProductRepository products;

    public ReviewService(ReviewRepository reviews, ProductRepository products) {
        this.reviews = reviews;
        this.products = products;
    }

    /**
     * Saves a review and updates the product's average rating in the same transaction: if any
     * step fails, nothing is saved.
     *
     * The product row is locked first, so two reviews of the same product are handled one after
     * the other. That makes the duplicate check reliable and keeps the rating count exact.
     */
    @Transactional
    public Review create(long userId, long productId, CreateReviewRequest request) {
        products.lockActiveProduct(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        if (!reviews.hasDeliveredPurchase(userId, productId)) {
            throw new ForbiddenException("You can review a product only after it has been delivered to you");
        }
        if (reviews.existsByUserIdAndProductId(userId, productId)) {
            throw new ConflictException("You have already reviewed this product");
        }

        Review review = reviews.save(new Review(productId, userId, request.rating(), request.comment()));
        reviews.refreshProductRating(productId);
        return review;
    }

    /** Every rating is listed; a comment only once it is approved. */
    @Transactional(readOnly = true)
    public PageResponse<PublicReview> listForProduct(long productId, int page, int size) {
        if (!products.existsByIdAndActiveTrue(productId)) {
            throw new NotFoundException("Product not found");
        }
        return PageResponse.from(reviews.findRowsByProductId(productId, pageRequest(page, size)),
                PublicReview::from);
    }

    static Pageable pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }
}
