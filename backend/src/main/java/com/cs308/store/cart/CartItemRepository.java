package com.cs308.store.cart;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, CartItemId> {

    /** The cart with everything the cart page shows, in one query, oldest line first. */
    @Query(value = """
            SELECT c.variant_id       AS "variantId",
                   p.id               AS "productId",
                   p.slug             AS "slug",
                   p.name             AS "name",
                   v.size             AS "size",
                   v.color            AS "color",
                   p.final_price      AS "price",
                   p.main_image_url   AS "image",
                   c.quantity         AS "quantity",
                   v.stock_quantity   AS "maxStock"
            FROM cart_items c
            JOIN product_variants v ON v.id = c.variant_id
            JOIN products p ON p.id = v.product_id
            WHERE c.user_id = :userId
            ORDER BY c.added_at, c.variant_id
            """, nativeQuery = true)
    List<CartLineView> findLines(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId")
    void deleteAllForUser(@Param("userId") Long userId);
}
