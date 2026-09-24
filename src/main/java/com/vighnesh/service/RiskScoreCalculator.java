package com.vighnesh.service;

import model.RiskFinding;
import model.RiskSeverity;

import java.util.List;

public class RiskScoreCalculator {

    public int calculateScore(List<RiskFinding> findings) {
        if (findings == null || findings.isEmpty()) {
            return 0;
        }

        int totalRisk = 0;
        for (RiskFinding finding : findings) {
            if (finding != null) {
                totalRisk += finding.getScore();
            }
        }

        return Math.min(totalRisk, 100);
    }

    public RiskSeverity determineSeverity(int score) {
        if (score >= 80) {
            return RiskSeverity.CRITICAL;
        } else if (score >= 60) {
            return RiskSeverity.HIGH;
        } else if (score >= 30) {
            return RiskSeverity.MEDIUM;
        } else {
            return RiskSeverity.LOW;
        }
    }
}
