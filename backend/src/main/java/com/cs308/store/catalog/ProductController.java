package com.cs308.store.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cs308.store.common.PageResponse;

/**
 * Public catalog endpoints. SecurityConfig lets anyone GET /api/products/**, so guests can
 * browse without an account (requirement 4).
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** GET /api/products?page=0&size=12 (page is zero-based) */
    @GetMapping
    public PageResponse<ProductSummary> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return productService.list(page, size);
    }

    /** GET /api/products/basic-tee-black */
    @GetMapping("/{slug}")
    public ProductDetailResponse get(@PathVariable String slug) {
        return productService.getBySlug(slug);
    }
}
