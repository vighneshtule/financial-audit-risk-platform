package rule;

import config.RiskConfiguration;
import model.RiskFinding;
import model.RiskSeverity;
import model.RiskType;
import model.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VendorConcentrationRule implements DatasetRiskRule {

    private static final double DEFAULT_THRESHOLD_PERCENT = 70.0;
    private static final int DEFAULT_MINIMUM_TRANSACTIONS = 3;
    private static final int DEFAULT_SCORE = 15;

    private final double thresholdPercent;
    private final int minimumTransactions;
    private final int score;

    public VendorConcentrationRule() {
        this(DEFAULT_THRESHOLD_PERCENT, DEFAULT_MINIMUM_TRANSACTIONS, DEFAULT_SCORE);
    }

    public VendorConcentrationRule(double thresholdPercent, int minimumTransactions, int score) {
        this.thresholdPercent = thresholdPercent;
        this.minimumTransactions = minimumTransactions;
        this.score = score;
    }

    public VendorConcentrationRule(RiskConfiguration.VendorConcentration config) {
        this(
                config != null ? config.getThresholdPercent() : DEFAULT_THRESHOLD_PERCENT,
                config != null ? config.getMinimumTransactions() : DEFAULT_MINIMUM_TRANSACTIONS,
                config != null ? config.getScore() : DEFAULT_SCORE
        );
    }

    @Override
    public RiskFinding evaluate(
            Transaction transaction,
            List<Transaction> transactions) {

        if (transaction == null
                || transaction.getEmployee() == null
                || transactions == null
                || transactions.isEmpty()) {
            return null;
        }

        int totalEmployeeTransactions = 0;
        Map<String, Integer> vendorCounts = new HashMap<>();

        for (Transaction t : transactions) {
            if (t == null || t.getEmployee() == null) {
                continue;
            }

            if (transaction.getEmployee().equals(t.getEmployee())) {
                totalEmployeeTransactions++;
                if (t.getVendor() != null) {
                    vendorCounts.put(t.getVendor(), vendorCounts.getOrDefault(t.getVendor(), 0) + 1);
                }
            }
        }

        if (totalEmployeeTransactions < minimumTransactions) {
            return null;
        }

        String highestVendor = null;
        int highestVendorCount = 0;

        for (Map.Entry<String, Integer> entry : vendorCounts.entrySet()) {
            if (entry.getValue() > highestVendorCount) {
                highestVendorCount = entry.getValue();
                highestVendor = entry.getKey();
            }
        }

        if (highestVendor == null || highestVendorCount == 0) {
            return null;
        }

        double concentration = ((double) highestVendorCount / totalEmployeeTransactions) * 100.0;

        if (concentration >= thresholdPercent) {
            String percentageStr = formatPercentage(concentration);
            return new RiskFinding(
                    RiskType.VENDOR_CONCENTRATION,
                    score,
                    RiskSeverity.MEDIUM,
                    "Employee transactions are " + percentageStr + "% concentrated with vendor " + highestVendor
            );
        }

        return null;
    }

    private String formatPercentage(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    public double getThresholdPercent() {
        return thresholdPercent;
    }

    public int getMinimumTransactions() {
        return minimumTransactions;
    }

    public int getScore() {
        return score;
    }
}
