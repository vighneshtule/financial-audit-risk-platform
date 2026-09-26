package com.vighnesh.controller;

import com.vighnesh.service.AnalyticsService;
import model.RiskIntelligenceSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk/intelligence")
public class RiskIntelligenceController {

    private final AnalyticsService analyticsService;

    public RiskIntelligenceController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<RiskIntelligenceSummary> getSummary() throws Exception {
        RiskIntelligenceSummary summary = analyticsService.getRiskIntelligenceSummary();
        return ResponseEntity.ok(summary);
    }
}
