package com.cs308.store.review;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cs308.store.common.PageResponse;

import jakarta.validation.Valid;

/**
 * Comment approval for product managers. The path is under /api/admin/products, which
 * SecurityConfig also limits to PRODUCT_MANAGER, so a forgotten annotation cannot open it.
 */
@RestController
@RequestMapping("/api/admin/products/reviews")
@PreAuthorize("hasRole('PRODUCT_MANAGER')")
public class ReviewModerationController {

    private final ReviewModerationService moderationService;

    public ReviewModerationController(ReviewModerationService moderationService) {
        this.moderationService = moderationService;
    }

    /** GET /api/admin/products/reviews: comments waiting for a decision, oldest first. */
    @GetMapping
    public PageResponse<ModerationItem> pending(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return moderationService.pendingComments(page, size);
    }

    /** PATCH /api/admin/products/reviews/12 with {"decision": "APPROVED"} */
    @PatchMapping("/{reviewId}")
    public ReviewResponse decide(@AuthenticationPrincipal Long moderatorId, @PathVariable long reviewId,
            @Valid @RequestBody ModerationRequest request) {
        return ReviewResponse.from(moderationService.decide(reviewId, moderatorId, request.decision()));
    }
}
