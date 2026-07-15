package com.epproject.ShortBrew.controller;

import com.epproject.ShortBrew.controller.dto.URLAnalyticsResponse;
import com.epproject.ShortBrew.controller.dto.UserAnalyticsSummary;
import com.epproject.ShortBrew.model.User;
import com.epproject.ShortBrew.security.CurrentUser;
import com.epproject.ShortBrew.security.RequireAuth;
import com.epproject.ShortBrew.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequireAuth
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/user/summary")
    public ResponseEntity<UserAnalyticsSummary> getUserSummary(@CurrentUser User currentUser) {
        UserAnalyticsSummary summary = analyticsService.getUserSummary(currentUser.id());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/urls/{url_id}")
    public ResponseEntity<URLAnalyticsResponse> getUrlAnalytics(
            @PathVariable("url_id") Long urlId,
            @RequestParam(name = "days", defaultValue = "30") int days,
            @CurrentUser User currentUser
    ) {
        URLAnalyticsResponse response = analyticsService.getUrlAnalytics(urlId, currentUser.id(), days);
        return ResponseEntity.ok(response);
    }
}
