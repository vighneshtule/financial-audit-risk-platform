package model;

/**
 * Summary returned by POST /api/risk/analyze-all.
 *
 * Deliberately separate from RiskSummary because RiskSummary
 * carries dashboard-specific fields (totalAmount, highestRiskTransactionId)
 * that are unrelated to a batch-analysis response.
 */
public class AnalysisResult {

    private final int transactionsAnalyzed;
    private final int lowRisk;
    private final int mediumRisk;
    private final int highRisk;
    private final int criticalRisk;
    private final int highestRiskScore;

    public AnalysisResult(
            int transactionsAnalyzed,
            int lowRisk,
            int mediumRisk,
            int highRisk,
            int criticalRisk,
            int highestRiskScore) {

        this.transactionsAnalyzed = transactionsAnalyzed;
        this.lowRisk = lowRisk;
        this.mediumRisk = mediumRisk;
        this.highRisk = highRisk;
        this.criticalRisk = criticalRisk;
        this.highestRiskScore = highestRiskScore;
    }

    public int getTransactionsAnalyzed() {
        return transactionsAnalyzed;
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
