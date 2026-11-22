package com.cs308.store.catalog;

/**
 * One active category as loaded by {@link CategoryRepository#findActiveRows()}.
 *
 * @param parentId null for a top-level category
 */
public record CategoryRow(Long id, String name, String slug, String description, Long parentId) {
}
