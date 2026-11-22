package com.cs308.store.catalog;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Deactivated products are hidden from customers. */
    Page<Product> findByActiveTrue(Pageable pageable);

    /** Products filed under any of the given categories (a category and its subcategories). */
    Page<Product> findByActiveTrueAndCategoryIdIn(Collection<Long> categoryIds, Pageable pageable);

    Optional<Product> findBySlugAndActiveTrue(String slug);

    boolean existsByIdAndActiveTrue(Long id);

    /**
     * Locks an active product's row until the current transaction ends and returns its id.
     * Another transaction that asks for the same lock waits here, so changes that read and then
     * write the product's numbers (rating, later stock) happen one after the other (requirement 18).
     */
    @Query(value = "SELECT id FROM products WHERE id = :id AND is_active FOR UPDATE", nativeQuery = true)
    Optional<Long> lockActiveProduct(@Param("id") Long id);
}
