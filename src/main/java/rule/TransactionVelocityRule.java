package rule;

import config.RiskConfiguration;
import model.RiskFinding;
import model.RiskSeverity;
import model.RiskType;
import model.Transaction;

import java.time.Duration;
import java.util.List;

public class TransactionVelocityRule implements DatasetRiskRule {

    private static final long DEFAULT_WINDOW_MINUTES = 30;
    private static final int DEFAULT_THRESHOLD = 3;
    private static final int DEFAULT_SCORE = 20;

    private final long windowMinutes;
    private final int threshold;
    private final int score;

    public TransactionVelocityRule() {
        this(DEFAULT_WINDOW_MINUTES, DEFAULT_THRESHOLD, DEFAULT_SCORE);
    }

    public TransactionVelocityRule(long windowMinutes, int threshold, int score) {
        this.windowMinutes = windowMinutes;
        this.threshold = threshold;
        this.score = score;
    }

    public TransactionVelocityRule(RiskConfiguration.TransactionVelocity config) {
        this(
                config != null ? config.getWindowMinutes() : DEFAULT_WINDOW_MINUTES,
                config != null ? config.getThreshold() : DEFAULT_THRESHOLD,
                config != null ? config.getScore() : DEFAULT_SCORE
        );
    }

    @Override
    public RiskFinding evaluate(
            Transaction transaction,
            List<Transaction> transactions) {

        if (transaction == null
                || transaction.getEmployee() == null
                || transaction.getTransactionTime() == null
                || transactions == null
                || transactions.isEmpty()) {
            return null;
        }

        int count = 0;
        for (Transaction other : transactions) {
            if (other == null
                    || other.getEmployee() == null
                    || other.getTransactionTime() == null) {
                continue;
            }

            if (!transaction.getEmployee().equals(other.getEmployee())) {
                continue;
            }

            long minutesDifference = Math.abs(
                    Duration.between(
                            transaction.getTransactionTime(),
                            other.getTransactionTime()
                    ).toMinutes()
            );

            if (minutesDifference <= windowMinutes) {
                count++;
            }
        }

        if (count >= threshold) {
            return new RiskFinding(
                    RiskType.TRANSACTION_VELOCITY,
                    score,
                    RiskSeverity.MEDIUM,
                    "Employee has " + threshold + " or more transactions within a " + windowMinutes + "-minute window"
            );
        }

        return null;
    }

    public long getWindowMinutes() {
        return windowMinutes;
    }

    public int getThreshold() {
        return threshold;
    }

    public int getScore() {
        return score;
    }
}
