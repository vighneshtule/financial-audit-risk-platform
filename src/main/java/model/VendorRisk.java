package model;

import java.math.BigDecimal;

public class VendorRisk {

    private final String vendor;
    private final int transactionCount;
    private final int flaggedTransactionCount;
    private final BigDecimal totalAmount;
    private final int highestRiskScore;
    private final int findingCount;

    public VendorRisk(
            String vendor,
            int transactionCount,
            int flaggedTransactionCount,
            BigDecimal totalAmount,
            int highestRiskScore,
            int findingCount) {

        this.vendor = vendor;
        this.transactionCount = transactionCount;
        this.flaggedTransactionCount = flaggedTransactionCount;
        this.totalAmount = totalAmount;
        this.highestRiskScore = highestRiskScore;
        this.findingCount = findingCount;
    }

    public String getVendor() {
        return vendor;
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
