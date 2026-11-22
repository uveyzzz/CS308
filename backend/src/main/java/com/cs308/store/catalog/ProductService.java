package com.cs308.store.catalog;

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

    /** Newest first until sorting (SCRUM-20) lets the user choose. The id breaks ties. */
    static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final ProductRepository products;
    private final ProductDetailsRepository details;
    private final ProductVariantRepository variants;

    public ProductService(ProductRepository products, ProductDetailsRepository details,
            ProductVariantRepository variants) {
        this.products = products;
        this.details = details;
        this.variants = variants;
    }

    public PageResponse<ProductSummary> list(int page, int size) {
        return PageResponse.from(products.findByActiveTrue(pageRequest(page, size)), ProductSummary::from);
    }

    public ProductDetailResponse getBySlug(String slug) {
        Product product = products.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        return ProductDetailResponse.from(product,
                details.findById(product.getId()).orElse(null),
                variants.findByProductIdOrderByIdAsc(product.getId()));
    }

    /** Out-of-range values are clamped instead of rejected, so a hand-typed URL still works. */
    static Pageable pageRequest(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, DEFAULT_SORT);
    }
}
