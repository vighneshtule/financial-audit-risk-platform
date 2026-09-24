package rule;

import config.RiskConfiguration;
import model.RiskFinding;
import model.RiskSeverity;
import model.RiskType;
import model.Transaction;

import java.math.BigDecimal;

public class RoundAmountRule implements RiskRule {

    private static final BigDecimal DEFAULT_UNIT = new BigDecimal("10000");
    private static final int DEFAULT_SCORE = 10;

    private final BigDecimal unit;
    private final int score;

    public RoundAmountRule() {
        this(DEFAULT_UNIT, DEFAULT_SCORE);
    }

    public RoundAmountRule(BigDecimal unit, int score) {
        this.unit = unit;
        this.score = score;
    }

    public RoundAmountRule(RiskConfiguration.RoundAmount config) {
        this(
                config != null ? config.getUnit() : DEFAULT_UNIT,
                config != null ? config.getScore() : DEFAULT_SCORE
        );
    }

    @Override
    public RiskFinding evaluate(Transaction transaction) {
        if (transaction == null || transaction.getAmount() == null || unit == null) {
            return null;
        }

        BigDecimal amount = transaction.getAmount();

        if (amount.compareTo(BigDecimal.ZERO) <= 0 || unit.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal remainder = amount.remainder(unit);
        if (remainder.compareTo(BigDecimal.ZERO) == 0) {
            return new RiskFinding(
                    RiskType.ROUND_AMOUNT,
                    score,
                    RiskSeverity.MEDIUM,
                    "Transaction amount is a round multiple of INR " + unit.toPlainString()
            );
        }

        return null;
    }

    public BigDecimal getUnit() {
        return unit;
    }

    public int getScore() {
        return score;
    }
}
