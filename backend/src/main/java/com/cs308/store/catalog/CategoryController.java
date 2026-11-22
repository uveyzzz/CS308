package com.cs308.store.catalog;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, like the rest of the catalog: SecurityConfig lets anyone GET /api/categories/**. */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** GET /api/categories: the menu tree. */
    @GetMapping
    public List<CategoryNode> tree() {
        return categoryService.tree();
    }

    /** GET /api/categories/women-tops: name, breadcrumb and subcategories of one category. */
    @GetMapping("/{slug}")
    public CategoryPageResponse get(@PathVariable String slug) {
        return categoryService.getBySlug(slug);
    }
}
