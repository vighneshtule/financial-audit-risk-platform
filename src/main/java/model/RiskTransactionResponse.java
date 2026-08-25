package model;

import java.math.BigDecimal;
import java.util.List;

public class RiskTransactionResponse {

    private final String transactionId;
    private final String vendor;
    private final String employee;
    private final BigDecimal amount;
    private final String category;

    private final int riskScore;
    private final RiskSeverity riskLevel;
    private final List<RiskFinding> findings;

    public RiskTransactionResponse(
            String transactionId,
            String vendor,
            String employee,
            BigDecimal amount,
            String category,
            int riskScore,
            RiskSeverity riskLevel,
            List<RiskFinding> findings) {

        this.transactionId = transactionId;
        this.vendor = vendor;
        this.employee = employee;
        this.amount = amount;
        this.category = category;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.findings = findings;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getVendor() {
        return vendor;
    }

    public String getEmployee() {
        return employee;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
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
