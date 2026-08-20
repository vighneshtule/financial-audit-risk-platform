package model;

import java.util.List;

public class RiskReport {

    private final int riskScore;
    private final RiskSeverity riskLevel;
    private final List<RiskFinding> findings;

    public RiskReport(
            int riskScore,
            RiskSeverity riskLevel,
            List<RiskFinding> findings) {

        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.findings = findings;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public RiskSeverity getRiskLevel() {
        return riskLevel;
    }

    public List<RiskFinding> getFindings() {
        return findings;
    }
}