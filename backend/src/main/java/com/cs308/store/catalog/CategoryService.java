package com.cs308.store.catalog;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cs308.store.common.NotFoundException;

/**
 * Category menu, category pages and the "products of this category" filter (SCRUM-16).
 *
 * A category is visible only if it and all of its parents are active: deactivating Women
 * also hides Women > Tops.
 */
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categories;

    public CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    /** The whole menu: top-level categories, each with its subcategories, sorted by name. */
    public List<CategoryNode> tree() {
        Tree tree = loadTree();
        return tree.nodesUnder(null);
    }

    public CategoryPageResponse getBySlug(String slug) {
        Tree tree = loadTree();
        CategoryRow category = tree.visibleBySlug(slug);
        return new CategoryPageResponse(category.id(), category.name(), category.slug(), category.description(),
                tree.pathTo(category), tree.nodesUnder(category.id()));
    }

    /**
     * The category and all categories below it. Listing Women must also show the products
     * filed under Women > Tops.
     */
    public Set<Long> subtreeIds(String slug) {
        Tree tree = loadTree();
        CategoryRow root = tree.visibleBySlug(slug);
        Set<Long> ids = new LinkedHashSet<>();
        Deque<Long> toVisit = new ArrayDeque<>(List.of(root.id()));
        while (!toVisit.isEmpty()) {
            Long id = toVisit.pop();
            if (ids.add(id)) {
                tree.childrenOf(id).forEach(child -> toVisit.push(child.id()));
            }
        }
        return ids;
    }

    private Tree loadTree() {
        return new Tree(categories.findActiveRows());
    }

    /** The active categories, indexed by id and by parent. */
    private static final class Tree {

        private final Map<Long, CategoryRow> byId = new LinkedHashMap<>();
        private final Map<Long, List<CategoryRow>> byParent = new LinkedHashMap<>();

        Tree(List<CategoryRow> rows) {
            for (CategoryRow row : rows) {
                byId.put(row.id(), row);
                byParent.computeIfAbsent(row.parentId(), key -> new ArrayList<>()).add(row);
            }
        }

        List<CategoryRow> childrenOf(Long parentId) {
            return byParent.getOrDefault(parentId, List.of());
        }

        /** Children of an inactive category are never reached, because the walk starts at visible ones. */
        List<CategoryNode> nodesUnder(Long parentId) {
            return childrenOf(parentId).stream()
                    .map(row -> new CategoryNode(row.id(), row.name(), row.slug(), nodesUnder(row.id())))
                    .toList();
        }

        CategoryRow visibleBySlug(String slug) {
            return byId.values().stream()
                    .filter(row -> row.slug().equals(slug))
                    .filter(this::allParentsActive)
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Category not found"));
        }

        List<CategoryPageResponse.Crumb> pathTo(CategoryRow category) {
            List<CategoryPageResponse.Crumb> path = new ArrayList<>();
            for (CategoryRow row = category; row != null; row = parentOf(row)) {
                path.add(new CategoryPageResponse.Crumb(row.name(), row.slug()));
            }
            Collections.reverse(path);
            return path;
        }

        /** Walks up to the top. Inactive categories are not in byId, so an inactive parent ends the walk. */
        private boolean allParentsActive(CategoryRow row) {
            CategoryRow current = row;
            for (int steps = 0; current.parentId() != null; steps++) {
                CategoryRow parent = byId.get(current.parentId());
                // steps guards against a loop in the data (A's parent is B, B's parent is A).
                if (parent == null || steps > byId.size()) {
                    return false;
                }
                current = parent;
            }
            return true;
        }

        private CategoryRow parentOf(CategoryRow row) {
            return row.parentId() == null ? null : byId.get(row.parentId());
        }
    }
}
