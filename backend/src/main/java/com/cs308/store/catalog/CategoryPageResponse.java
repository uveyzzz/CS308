package com.cs308.store.catalog;

import java.util.List;

/**
 * What the category page shows above its products.
 *
 * @param path     the breadcrumb, from the top-level category down to this one (this one included)
 * @param children the direct subcategories, for quick links
 */
public record CategoryPageResponse(Long id, String name, String slug, String description, List<Crumb> path,
        List<CategoryNode> children) {

    public record Crumb(String name, String slug) {
    }
}
