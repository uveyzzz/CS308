package com.cs308.store.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    /** A product's reviews with the author's name, newest first. */
    @Query(value = """
            SELECT new com.cs308.store.review.ReviewRow(r.id, r.rating, r.comment, r.commentStatus,
                   u.fullName, r.createdAt)
            FROM Review r JOIN User u ON u.id = r.userId
            WHERE r.productId = :productId
            ORDER BY r.createdAt DESC, r.id DESC
            """,
            countQuery = "SELECT count(r) FROM Review r WHERE r.productId = :productId")
    Page<ReviewRow> findRowsByProductId(@Param("productId") Long productId, Pageable pageable);

    /**
     * True if the user has an order with status DELIVERED that contains any variant of the product
     * (requirement 5: only delivered products can be reviewed).
     *
     * Uses the orders and order_items tables from SCRUM-29, so it only works once that migration is in.
     */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM orders o
                JOIN order_items oi ON oi.order_id = o.id
                JOIN product_variants v ON v.id = oi.variant_id
                WHERE o.user_id = :userId AND o.status = 'DELIVERED' AND v.product_id = :productId
            )
            """, nativeQuery = true)
    boolean hasDeliveredPurchase(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * Recalculates the product's average rating and rating count from its reviews.
     *
     * Counting again instead of adding one keeps the average exact. ReviewService locks the
     * product row first, so two reviews of the same product never recalculate at the same time.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE products
            SET rating_count = (SELECT COUNT(*) FROM reviews WHERE product_id = :productId),
                avg_rating   = COALESCE((SELECT ROUND(AVG(rating), 2) FROM reviews WHERE product_id = :productId), 0)
            WHERE id = :productId
            """, nativeQuery = true)
    int refreshProductRating(@Param("productId") Long productId);
}
