package com.ekart.ekart.controller;

import com.ekart.ekart.config.AdminGuard;
import com.ekart.ekart.dto.*;
import com.ekart.ekart.service.AiAssistantService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiAssistantService aiAssistantService;
    private final AdminGuard adminGuard;

    public AiController(AiAssistantService aiAssistantService, AdminGuard adminGuard) {
        this.aiAssistantService = aiAssistantService;
        this.adminGuard = adminGuard;
    }

    @PostMapping("/goal-to-cart")
    public GoalToCartResponse goalToCart(@RequestBody GoalToCartRequest request) {
        return aiAssistantService.goalToCart(request);
    }

    @PostMapping("/compare")
    public CompareResponse compare(@RequestBody CompareRequest request) {
        return aiAssistantService.compare(request);
    }

    @GetMapping("/review-summary/{productId}")
    public ReviewSummaryResponse reviewSummary(@PathVariable Long productId) {
        return aiAssistantService.summarizeReviews(productId);
    }

    @GetMapping("/admin/insights")
    public AdminInsightsResponse adminInsights(
            @RequestHeader(value = "X-User-Id", required = false) Long adminId) {

        adminGuard.require(adminId);
        return aiAssistantService.adminInsights();
    }
}
