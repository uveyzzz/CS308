package com.cs308.store.catalog;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cs308.store.common.NotFoundException;
import com.cs308.store.common.PageResponse;

/** Read side of the catalog: what guests and customers see. Changing the catalog is not done here. */
@Service
@Transactional(readOnly = true)
public class ProductService {

    static final int MAX_PAGE_SIZE = 48;

    /** Used when the request does not choose a sort. */
    static final Sort DEFAULT_SORT = ProductSort.NEWEST.toSort();

    private final ProductRepository products;
    private final ProductDetailsRepository details;
    private final ProductVariantRepository variants;
    private final CategoryService categoryService;

    public ProductService(ProductRepository products, ProductDetailsRepository details,
            ProductVariantRepository variants, CategoryService categoryService) {
        this.products = products;
        this.details = details;
        this.variants = variants;
        this.categoryService = categoryService;
    }

    public PageResponse<ProductSummary> list(int page, int size, String categorySlug) {
        return list(page, size, categorySlug, ProductSort.NEWEST);
    }

    public PageResponse<ProductSummary> list(int page, int size, String categorySlug, ProductSort sort) {
        return list(page, size, categorySlug, sort, null);
    }

    /**
     * @param categorySlug null or blank lists every product; otherwise only that category and
     *                     its subcategories. An unknown category is a 404, not an empty list.
     * @param sort         the order chosen by the user (SCRUM-20); null means newest first.
     * @param search       what the user typed in the search bar (SCRUM-19). Matches the name or the
     *                     description, ignoring case. null or blank means no search.
     */
    public PageResponse<ProductSummary> list(int page, int size, String categorySlug, ProductSort sort,
            String search) {
        Pageable pageable = pageRequest(page, size, sort);
        String pattern = SearchText.toPattern(search);
        Page<Product> result;
        if (categorySlug == null || categorySlug.isBlank()) {
            result = pattern == null
                    ? products.findByActiveTrue(pageable)
                    : products.search(pattern, pageable);
        } else {
            Set<Long> categoryIds = categoryService.subtreeIds(categorySlug);
            result = pattern == null
                    ? products.findByActiveTrueAndCategoryIdIn(categoryIds, pageable)
                    : products.searchInCategories(pattern, categoryIds, pageable);
        }
        return PageResponse.from(result, ProductSummary::from);
    }

    public ProductDetailResponse getBySlug(String slug) {
        Product product = products.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        return ProductDetailResponse.from(product,
                details.findById(product.getId()).orElse(null),
                variants.findByProductIdOrderByIdAsc(product.getId()));
    }

    static Pageable pageRequest(int page, int size) {
        return pageRequest(page, size, ProductSort.NEWEST);
    }

    /** Out-of-range values are clamped instead of rejected, so a hand-typed URL still works. */
    static Pageable pageRequest(int page, int size, ProductSort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        ProductSort safeSort = sort == null ? ProductSort.NEWEST : sort;
        return PageRequest.of(safePage, safeSize, safeSort.toSort());
    }
}