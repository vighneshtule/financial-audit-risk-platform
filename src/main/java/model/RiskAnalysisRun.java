package model;

import java.time.LocalDateTime;

public class RiskAnalysisRun {

    private final long id;
    private final String transactionId;
    private final int riskScore;
    private final RiskSeverity riskLevel;
    private final LocalDateTime analyzedAt;

    public RiskAnalysisRun(
            long id,
            String transactionId,
            int riskScore,
            RiskSeverity riskLevel,
            LocalDateTime analyzedAt) {

        this.id = id;
        this.transactionId = transactionId;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.analyzedAt = analyzedAt;
    }

    public long getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public RiskSeverity getRiskLevel() {
        return riskLevel;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }
}
