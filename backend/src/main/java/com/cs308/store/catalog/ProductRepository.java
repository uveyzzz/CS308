package com.cs308.store.catalog;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Deactivated products are hidden from customers. */
    Page<Product> findByActiveTrue(Pageable pageable);

    /** Products filed under any of the given categories (a category and its subcategories). */
    Page<Product> findByActiveTrueAndCategoryIdIn(Collection<Long> categoryIds, Pageable pageable);

    Optional<Product> findBySlugAndActiveTrue(String slug);
}
