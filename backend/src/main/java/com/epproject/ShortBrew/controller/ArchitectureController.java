package com.epproject.ShortBrew.controller;

import com.epproject.ShortBrew.controller.dto.SystemMetricsResponse;
import com.epproject.ShortBrew.service.SystemMetricsService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/architecture")
public class ArchitectureController {

    private final SystemMetricsService metricsService;

    public ArchitectureController(SystemMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<SystemMetricsResponse> getMetrics() {
        SystemMetricsResponse metrics = metricsService.getMetrics();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(metrics);
    }
}
