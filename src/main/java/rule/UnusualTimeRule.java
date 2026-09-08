package rule;

import config.RiskConfiguration;
import model.RiskFinding;
import model.RiskSeverity;
import model.RiskType;
import model.Transaction;

import java.time.LocalTime;

public class UnusualTimeRule implements RiskRule {

    private static final LocalTime DEFAULT_BUSINESS_START =
            LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_BUSINESS_END =
            LocalTime.of(18, 0);
    private static final int DEFAULT_SCORE = 20;

    private final LocalTime businessStart;
    private final LocalTime businessEnd;
    private final int score;

    public UnusualTimeRule() {
        this(DEFAULT_BUSINESS_START, DEFAULT_BUSINESS_END, DEFAULT_SCORE);
    }

    public UnusualTimeRule(LocalTime businessStart, LocalTime businessEnd, int score) {
        this.businessStart = businessStart;
        this.businessEnd = businessEnd;
        this.score = score;
    }

    public UnusualTimeRule(RiskConfiguration.UnusualTime config) {
        this(
                config != null ? config.getBusinessStart() : DEFAULT_BUSINESS_START,
                config != null ? config.getBusinessEnd() : DEFAULT_BUSINESS_END,
                config != null ? config.getScore() : DEFAULT_SCORE
        );
    }

    @Override
    public RiskFinding evaluate(Transaction transaction) {

        LocalTime transactionTime =
                transaction.getTransactionTime().toLocalTime();

        if (transactionTime.isBefore(businessStart)
                || transactionTime.isAfter(businessEnd)) {

            return new RiskFinding(
                    RiskType.UNUSUAL_TRANSACTION_TIME,
                    score,
                    RiskSeverity.MEDIUM,
                    "Transaction occurred outside normal business hours"
            );
        }

        return null;
    }

    public LocalTime getBusinessStart() {
        return businessStart;
    }

    public LocalTime getBusinessEnd() {
        return businessEnd;
    }

    public int getScore() {
        return score;
    }
}