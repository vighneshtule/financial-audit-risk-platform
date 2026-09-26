package model;

import java.math.BigDecimal;

public class EvidenceSummary {
    private int relatedTransactionCount;
    private int relatedFlaggedTransactionCount;
    private BigDecimal relatedAmount;

    public EvidenceSummary() {}

    public EvidenceSummary(int relatedTransactionCount, int relatedFlaggedTransactionCount, BigDecimal relatedAmount) {
        this.relatedTransactionCount = relatedTransactionCount;
        this.relatedFlaggedTransactionCount = relatedFlaggedTransactionCount;
        this.relatedAmount = relatedAmount;
    }

    public int getRelatedTransactionCount() {
        return relatedTransactionCount;
    }

    public void setRelatedTransactionCount(int relatedTransactionCount) {
        this.relatedTransactionCount = relatedTransactionCount;
    }

    public int getRelatedFlaggedTransactionCount() {
        return relatedFlaggedTransactionCount;
    }

    public void setRelatedFlaggedTransactionCount(int relatedFlaggedTransactionCount) {
        this.relatedFlaggedTransactionCount = relatedFlaggedTransactionCount;
    }

    public BigDecimal getRelatedAmount() {
        return relatedAmount;
    }

    public void setRelatedAmount(BigDecimal relatedAmount) {
        this.relatedAmount = relatedAmount;
    }
}
