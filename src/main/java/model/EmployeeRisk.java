package model;

import java.math.BigDecimal;

public class EmployeeRisk {

    private final String employee;
    private final int transactionCount;
    private final int flaggedTransactionCount;
    private final BigDecimal totalAmount;
    private final int highestRiskScore;
    private final int findingCount;

    public EmployeeRisk(
            String employee,
            int transactionCount,
            int flaggedTransactionCount,
            BigDecimal totalAmount,
            int highestRiskScore,
            int findingCount) {

        this.employee = employee;
        this.transactionCount = transactionCount;
        this.flaggedTransactionCount = flaggedTransactionCount;
        this.totalAmount = totalAmount;
        this.highestRiskScore = highestRiskScore;
        this.findingCount = findingCount;
    }

    public String getEmployee() {
        return employee;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public int getFlaggedTransactionCount() {
        return flaggedTransactionCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public int getHighestRiskScore() {
        return highestRiskScore;
    }

    public int getFindingCount() {
        return findingCount;
    }
}
