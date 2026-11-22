package com.cs308.store.review;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cs308.store.common.PageResponse;

import jakarta.validation.Valid;

/**
 * Reviews of one product. Anyone can read them; SecurityConfig lets only customers post.
 * The author always comes from the token, never from the request body.
 */
@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public PageResponse<PublicReview> list(@PathVariable long productId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return reviewService.listForProduct(productId, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@AuthenticationPrincipal Long userId, @PathVariable long productId,
            @Valid @RequestBody CreateReviewRequest request) {
        return ReviewResponse.from(reviewService.create(userId, productId, request));
    }
}
