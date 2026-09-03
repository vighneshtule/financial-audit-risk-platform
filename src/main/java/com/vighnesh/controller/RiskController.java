package com.vighnesh.controller;

import model.RiskAnalysisHistoryResponse;
import model.RiskAnalysisHistoryItem;
import model.RiskAnalysisHistoryPage;
import model.RiskFinding;
import model.RiskReport;

import model.RiskSeverity;
import model.RiskSummary;
import model.RiskTransactionResponse;
import com.vighnesh.service.RiskAnalysisService;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import model.RiskTransactionPage;

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

    @GetMapping("/transactions/{transactionId}/findings")
    public ResponseEntity<List<RiskFinding>> getPersistedFindings(
            @PathVariable String transactionId)
            throws Exception {

        return ResponseEntity.ok(
                riskAnalysisService.getPersistedFindings(
                        transactionId
                )
        );
    }

    @GetMapping("/transactions/{transactionId}/history")
    public ResponseEntity<RiskAnalysisHistoryResponse>
    getTransactionRiskHistory(
            @PathVariable String transactionId)
            throws Exception {

        return ResponseEntity.ok(
                riskAnalysisService.getTransactionRiskHistory(
                        transactionId
                )
        );
    }

    @PostMapping("/analyze/{transactionId}")
    public ResponseEntity<RiskReport> analyzeAndPersistTransaction(
            @PathVariable String transactionId)
            throws Exception {

        RiskReport report =
                riskAnalysisService.analyzeAndPersistTransaction(
                        transactionId
                );

        return ResponseEntity.ok(report);
    }

    @GetMapping("/transactions")
    public ResponseEntity<RiskTransactionPage> getRiskTransactions(
            @RequestParam(required = false)
            RiskSeverity riskLevel,

            @RequestParam(required = false)
            Integer minScore,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size)
            throws Exception {

        return ResponseEntity.ok(
                riskAnalysisService.analyzeTransactions(
                        riskLevel,
                        minScore,
                        page,
                        size
                )
        );
    }

    @GetMapping("/summary")
    public ResponseEntity<RiskSummary> getRiskSummary()
            throws Exception {

        return ResponseEntity.ok(
                riskAnalysisService.analyzeSummary()
        );
    }

    @GetMapping("/transactions/{transactionId}/history/{analysisRunId}")
    public ResponseEntity<RiskAnalysisHistoryItem>
    getTransactionRiskHistoryRun(
            @PathVariable String transactionId,
            @PathVariable long analysisRunId)
            throws Exception {

        return ResponseEntity.ok(
                riskAnalysisService.getTransactionRiskHistoryRun(
                        transactionId,
                        analysisRunId
                )
        );
    }

    @GetMapping("/transactions/{transactionId}/history/page")
    public ResponseEntity<RiskAnalysisHistoryPage>
    getTransactionRiskHistoryPage(
            @PathVariable String transactionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size)
            throws Exception {

        return ResponseEntity.ok(
                riskAnalysisService.getTransactionRiskHistoryPage(
                        transactionId,
                        page,
                        size
                )
        );
    }
}