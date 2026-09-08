package rule;

import config.RiskConfiguration;
import model.RiskFinding;
import model.RiskSeverity;
import model.RiskType;
import model.Transaction;

import java.math.BigDecimal;

public class HighAmountRule implements RiskRule {

    private static final BigDecimal DEFAULT_THRESHOLD =
            new BigDecimal("100000");
    private static final int DEFAULT_SCORE = 30;

    private final BigDecimal threshold;
    private final int score;

    public HighAmountRule() {
        this(DEFAULT_THRESHOLD, DEFAULT_SCORE);
    }

    public HighAmountRule(BigDecimal threshold, int score) {
        this.threshold = threshold;
        this.score = score;
    }

    public HighAmountRule(RiskConfiguration.HighAmount config) {
        this(
                config != null ? config.getThreshold() : DEFAULT_THRESHOLD,
                config != null ? config.getScore() : DEFAULT_SCORE
        );
    }

    @Override
    public RiskFinding evaluate(Transaction transaction) {

        if (transaction.getAmount().compareTo(threshold) > 0) {

            return new RiskFinding(
                    RiskType.HIGH_AMOUNT,
                    score,
                    RiskSeverity.MEDIUM,
                    "Transaction amount exceeds INR 1,00,000"
            );
        }

        return null;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public int getScore() {
        return score;
    }
}