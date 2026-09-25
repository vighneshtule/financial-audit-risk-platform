package model;

import java.math.BigDecimal;

public class DatasetOverview {

    private final int totalTransactions;
    private final int analyzedTransactions;
    private final BigDecimal totalAuditValue;
    private final int totalFindings;
    private final int lowRisk;
    private final int mediumRisk;
    private final int highRisk;
    private final int criticalRisk;
    private final int highestRiskScore;

    public DatasetOverview(
            int totalTransactions,
            int analyzedTransactions,
            BigDecimal totalAuditValue,
            int totalFindings,
            int lowRisk,
            int mediumRisk,
            int highRisk,
            int criticalRisk,
            int highestRiskScore) {

        this.totalTransactions = totalTransactions;
        this.analyzedTransactions = analyzedTransactions;
        this.totalAuditValue = totalAuditValue;
        this.totalFindings = totalFindings;
        this.lowRisk = lowRisk;
        this.mediumRisk = mediumRisk;
        this.highRisk = highRisk;
        this.criticalRisk = criticalRisk;
        this.highestRiskScore = highestRiskScore;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public int getAnalyzedTransactions() {
        return analyzedTransactions;
    }

    public BigDecimal getTotalAuditValue() {
        return totalAuditValue;
    }

    public int getTotalFindings() {
        return totalFindings;
    }

    public int getLowRisk() {
        return lowRisk;
    }

    public int getMediumRisk() {
        return mediumRisk;
    }

    public int getHighRisk() {
        return highRisk;
    }

    public int getCriticalRisk() {
        return criticalRisk;
    }

    public int getHighestRiskScore() {
        return highestRiskScore;
    }
}
