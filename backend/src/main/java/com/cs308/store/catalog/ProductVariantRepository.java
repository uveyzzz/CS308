package com.cs308.store.catalog;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdOrderByIdAsc(Long productId);

    /** Price, cost and names of the given variants in one query, for placing an order (SCRUM-26). */
    @Query(value = """
            SELECT v.id                                      AS "variantId",
                   p.name                                    AS "productName",
                   v.size                                    AS "size",
                   v.color                                   AS "color",
                   p.final_price                             AS "unitPrice",
                   p.discount_rate                           AS "discountRate",
                   COALESCE(pc.cost, ROUND(p.price * 0.5, 2)) AS "unitCost",
                   p.is_active                               AS "active"
            FROM product_variants v
            JOIN products p ON p.id = v.product_id
            LEFT JOIN product_costs pc ON pc.product_id = p.id
            WHERE v.id IN (:ids)
            """, nativeQuery = true)
    List<OrderableVariant> findOrderable(@Param("ids") Collection<Long> ids);

    /**
     * Takes quantity items out of stock in one statement, only if that many are left (requirements 3, 18).
     * The database checks and changes the row in one step, so two customers buying the last item
     * at the same time cannot both succeed: the second one updates 0 rows.
     *
     * @return 1 if the stock was decreased, 0 if there was not enough
     */
    @Modifying
    @Query(value = """
            UPDATE product_variants
            SET stock_quantity = stock_quantity - :quantity
            WHERE id = :id AND stock_quantity >= :quantity
            """, nativeQuery = true)
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);
}
