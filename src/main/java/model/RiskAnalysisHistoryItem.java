package model;

import java.time.LocalDateTime;
import java.util.List;

public class RiskAnalysisHistoryItem {

    private final long analysisRunId;
    private final LocalDateTime analyzedAt;
    private final int riskScore;
    private final RiskSeverity riskLevel;
    private final List<RiskFinding> findings;

    public RiskAnalysisHistoryItem(
            long analysisRunId,
            LocalDateTime analyzedAt,
            int riskScore,
            RiskSeverity riskLevel,
            List<RiskFinding> findings) {

        this.analysisRunId = analysisRunId;
        this.analyzedAt = analyzedAt;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.findings = findings;
    }

    public long getAnalysisRunId() {
        return analysisRunId;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
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
