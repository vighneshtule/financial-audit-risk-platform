package model;

import java.math.BigDecimal;

public class RelatedTransaction {
    private String transactionId;
    private BigDecimal amount;
    private String transactionTime;
    private Integer riskScore;
    private String riskLevel;

    public RelatedTransaction() {}

    public RelatedTransaction(String transactionId, BigDecimal amount, String transactionTime, Integer riskScore, String riskLevel) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.transactionTime = transactionTime;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(String transactionTime) {
        this.transactionTime = transactionTime;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}
