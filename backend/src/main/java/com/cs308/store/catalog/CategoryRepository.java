package com.cs308.store.catalog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Every active category with its parent's id. The table is small, so the service loads all
     * of it once and builds the menu, breadcrumbs and subtrees in memory instead of querying
     * one level at a time.
     */
    @Query("""
            SELECT new com.cs308.store.catalog.CategoryRow(c.id, c.name, c.slug, c.description, p.id)
            FROM Category c LEFT JOIN c.parent p
            WHERE c.active = true
            ORDER BY c.name
            """)
    List<CategoryRow> findActiveRows();
}
