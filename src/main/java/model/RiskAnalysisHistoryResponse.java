package model;

import java.util.List;

public class RiskAnalysisHistoryResponse {

    private final String transactionId;
    private final List<RiskAnalysisHistoryItem> analysisRuns;

    public RiskAnalysisHistoryResponse(
            String transactionId,
            List<RiskAnalysisHistoryItem> analysisRuns) {

        this.transactionId = transactionId;
        this.analysisRuns = analysisRuns;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public List<RiskAnalysisHistoryItem> getAnalysisRuns() {
        return analysisRuns;
    }
}
