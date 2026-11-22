package com.cs308.store.catalog;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * The search condition shared by the two search queries below (SCRUM-19). A product matches
     * when its name or its description contains the pattern. translate() turns the Turkish capitals
     * into small letters and treats I and ı as i before lower(), the same way as SearchText.fold,
     * so the result does not depend on the database's language settings. Stock is not checked:
     * out-of-stock products are still found (requirement 7).
     */
    String SEARCH_MATCH = """
            p.active = true AND (
                lower(cast(function('translate', p.name, 'ÇĞİIÖŞÜı', 'çğiiöşüi') as String))
                    LIKE :pattern ESCAPE '!'
                OR EXISTS (SELECT d FROM ProductDetails d WHERE d.productId = p.id
                    AND lower(cast(function('translate', d.description, 'ÇĞİIÖŞÜı', 'çğiiöşüi') as String))
                        LIKE :pattern ESCAPE '!'))
            """;

    /** Deactivated products are hidden from customers. */
    Page<Product> findByActiveTrue(Pageable pageable);

    /** Products filed under any of the given categories (a category and its subcategories). */
    Page<Product> findByActiveTrueAndCategoryIdIn(Collection<Long> categoryIds, Pageable pageable);

    /** @param pattern built by SearchText.toPattern, e.g. "%gömlek%" */
    @Query(value = "SELECT p FROM Product p WHERE " + SEARCH_MATCH,
            countQuery = "SELECT count(p) FROM Product p WHERE " + SEARCH_MATCH)
    Page<Product> search(@Param("pattern") String pattern, Pageable pageable);

    /** Search inside a category and its subcategories. */
    @Query(value = "SELECT p FROM Product p WHERE p.category.id IN :categoryIds AND " + SEARCH_MATCH,
            countQuery = "SELECT count(p) FROM Product p WHERE p.category.id IN :categoryIds AND " + SEARCH_MATCH)
    Page<Product> searchInCategories(@Param("pattern") String pattern,
            @Param("categoryIds") Collection<Long> categoryIds, Pageable pageable);

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
