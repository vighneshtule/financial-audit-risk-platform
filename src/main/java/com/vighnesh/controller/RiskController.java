package com.vighnesh.controller;

import model.RiskReport;
import model.RiskSummary;
import com.vighnesh.service.RiskAnalysisService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/risk")
public class RiskController {

    private final RiskAnalysisService riskAnalysisService;

    public RiskController(
            RiskAnalysisService riskAnalysisService) {

        this.riskAnalysisService = riskAnalysisService;
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<RiskReport> analyzeTransaction(
            @PathVariable String transactionId)
            throws Exception {

        RiskReport report =
                riskAnalysisService.analyzeTransaction(transactionId);

        return ResponseEntity.ok(report);
    }

    @GetMapping("/summary")
    public ResponseEntity<RiskSummary> getRiskSummary()
            throws Exception {

        return ResponseEntity.ok(
                riskAnalysisService.analyzeSummary()
        );
    }
}