package model;

import java.math.BigDecimal;

public class RiskSummary {

    private final int totalTransactions;
    private final BigDecimal totalAmount;

    private final int lowRiskTransactions;
    private final int mediumRiskTransactions;
    private final int highRiskTransactions;
    private final int criticalRiskTransactions;

    private final int totalFindings;

    private final String highestRiskTransactionId;
    private final int highestRiskScore;

    public RiskSummary(
            int totalTransactions,
            BigDecimal totalAmount,
            int lowRiskTransactions,
            int mediumRiskTransactions,
            int highRiskTransactions,
            int criticalRiskTransactions,
            int totalFindings,
            String highestRiskTransactionId,
            int highestRiskScore) {

        this.totalTransactions = totalTransactions;
        this.totalAmount = totalAmount;
        this.lowRiskTransactions = lowRiskTransactions;
        this.mediumRiskTransactions = mediumRiskTransactions;
        this.highRiskTransactions = highRiskTransactions;
        this.criticalRiskTransactions = criticalRiskTransactions;
        this.totalFindings = totalFindings;
        this.highestRiskTransactionId = highestRiskTransactionId;
        this.highestRiskScore = highestRiskScore;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public int getLowRiskTransactions() {
        return lowRiskTransactions;
    }

    public int getMediumRiskTransactions() {
        return mediumRiskTransactions;
    }

    public int getHighRiskTransactions() {
        return highRiskTransactions;
    }

    public int getCriticalRiskTransactions() {
        return criticalRiskTransactions;
    }

    public int getTotalFindings() {
        return totalFindings;
    }

    public String getHighestRiskTransactionId() {
        return highestRiskTransactionId;
    }

    public int getHighestRiskScore() {
        return highestRiskScore;
    }
}
