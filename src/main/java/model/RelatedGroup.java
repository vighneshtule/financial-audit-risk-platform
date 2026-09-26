package model;

import java.math.BigDecimal;
import java.util.List;

public class RelatedGroup {
    private String name;
    private int relatedTransactionCount;
    private BigDecimal totalAmount;
    private int highestRiskScore;
    private int flaggedTransactionCount;
    private List<RelatedTransaction> transactions;

    public RelatedGroup() {}

    public RelatedGroup(String name, int relatedTransactionCount, BigDecimal totalAmount, int highestRiskScore, int flaggedTransactionCount, List<RelatedTransaction> transactions) {
        this.name = name;
        this.relatedTransactionCount = relatedTransactionCount;
        this.totalAmount = totalAmount;
        this.highestRiskScore = highestRiskScore;
        this.flaggedTransactionCount = flaggedTransactionCount;
        this.transactions = transactions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRelatedTransactionCount() {
        return relatedTransactionCount;
    }

    public void setRelatedTransactionCount(int relatedTransactionCount) {
        this.relatedTransactionCount = relatedTransactionCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getHighestRiskScore() {
        return highestRiskScore;
    }

    public void setHighestRiskScore(int highestRiskScore) {
        this.highestRiskScore = highestRiskScore;
    }

    public int getFlaggedTransactionCount() {
        return flaggedTransactionCount;
    }

    public void setFlaggedTransactionCount(int flaggedTransactionCount) {
        this.flaggedTransactionCount = flaggedTransactionCount;
    }

    public List<RelatedTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<RelatedTransaction> transactions) {
        this.transactions = transactions;
    }
}
