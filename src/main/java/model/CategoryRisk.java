package model;

import java.math.BigDecimal;

public class CategoryRisk {

    private final String category;
    private final int transactionCount;
    private final int flaggedTransactionCount;
    private final BigDecimal totalAmount;
    private final int findingCount;

    public CategoryRisk(
            String category,
            int transactionCount,
            int flaggedTransactionCount,
            BigDecimal totalAmount,
            int findingCount) {

        this.category = category;
        this.transactionCount = transactionCount;
        this.flaggedTransactionCount = flaggedTransactionCount;
        this.totalAmount = totalAmount;
        this.findingCount = findingCount;
    }

    public String getCategory() {
        return category;
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

    public int getFindingCount() {
        return findingCount;
    }
}
