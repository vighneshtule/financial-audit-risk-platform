package rule;

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

    private static final long DUPLICATE_WINDOW_MINUTES = 10;

    private final Map<List<Transaction>, DuplicateTransactionIndex> cache =
            Collections.synchronizedMap(new WeakHashMap<>());

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

            if (minutesDifference <= DUPLICATE_WINDOW_MINUTES) {
                return new RiskFinding(
                        RiskType.DUPLICATE_TRANSACTION,
                        25,
                        RiskSeverity.MEDIUM,
                        "Possible duplicate transaction detected"
                );
            }
        }

        return null;
    }
}