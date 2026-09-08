package rule;

import config.RiskConfiguration;
import model.RiskFinding;
import model.RiskSeverity;
import model.RiskType;
import model.Transaction;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class DuplicateTransactionRule implements DatasetRiskRule {

    private static final long DEFAULT_DUPLICATE_WINDOW_MINUTES = 10;
    private static final int DEFAULT_SCORE = 25;

    private final long duplicateWindowMinutes;
    private final int score;

    private final Map<List<Transaction>, DuplicateTransactionIndex> cache =
            Collections.synchronizedMap(new WeakHashMap<>());

    public DuplicateTransactionRule() {
        this(DEFAULT_DUPLICATE_WINDOW_MINUTES, DEFAULT_SCORE);
    }

    public DuplicateTransactionRule(long duplicateWindowMinutes, int score) {
        this.duplicateWindowMinutes = duplicateWindowMinutes;
        this.score = score;
    }

    public DuplicateTransactionRule(RiskConfiguration.Duplicate config) {
        this(
                config != null ? config.getWindowMinutes() : DEFAULT_DUPLICATE_WINDOW_MINUTES,
                config != null ? config.getScore() : DEFAULT_SCORE
        );
    }

    @Override
    public RiskFinding evaluate(
            Transaction transaction,
            List<Transaction> transactions) {

        if (transaction == null || transactions == null || transactions.isEmpty()) {
            return null;
        }

        DuplicateTransactionIndex index = cache.computeIfAbsent(
                transactions,
                DuplicateTransactionIndex::from
        );

        List<Transaction> possibleDuplicates =
                index.getPossibleDuplicates(transaction);

        for (Transaction other : possibleDuplicates) {

            if (transaction.getId().equals(other.getId())) {
                continue;
            }

            long minutesDifference = Math.abs(
                    Duration.between(
                            transaction.getTransactionTime(),
                            other.getTransactionTime()
                    ).toMinutes()
            );

            if (minutesDifference <= duplicateWindowMinutes) {
                return new RiskFinding(
                        RiskType.DUPLICATE_TRANSACTION,
                        score,
                        RiskSeverity.MEDIUM,
                        "Possible duplicate transaction detected"
                );
            }
        }

        return null;
    }

    public long getDuplicateWindowMinutes() {
        return duplicateWindowMinutes;
    }

    public int getScore() {
        return score;
    }
}