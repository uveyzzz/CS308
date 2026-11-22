package com.cs308.store.catalog;

import java.util.List;

/** One entry of the category menu, with its subcategories. */
public record CategoryNode(Long id, String name, String slug, List<CategoryNode> children) {
}
