package rule;

import model.RiskFinding;
import model.RiskSeverity;
import model.RiskType;
import model.Transaction;
import model.TransactionKey;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DuplicateTransactionRule implements DatasetRiskRule {

        private static final long DUPLICATE_WINDOW_MINUTES = 10;

        @Override
        public RiskFinding evaluate(
                        Transaction transaction,
                        List<Transaction> transactions) {

                TransactionKey targetKey = new TransactionKey(
                                transaction.getVendor(),
                                transaction.getEmployee(),
                                transaction.getAmount(),
                                transaction.getCategory());

                Map<TransactionKey, List<Transaction>> groups = new HashMap<>();

                for (Transaction current : transactions) {

                        TransactionKey key = new TransactionKey(
                                        current.getVendor(),
                                        current.getEmployee(),
                                        current.getAmount(),
                                        current.getCategory());

                        groups
                                        .computeIfAbsent(
                                                        key,
                                                        k -> new ArrayList<>())
                                        .add(current);
                }

                List<Transaction> possibleDuplicates = groups.getOrDefault(
                                targetKey,
                                List.of());

                for (Transaction other : possibleDuplicates) {

                        if (transaction.getId()
                                        .equals(other.getId())) {

                                continue;
                        }

                        long minutesDifference = Math.abs(
                                        Duration.between(
                                                        transaction.getTransactionTime(),
                                                        other.getTransactionTime()).toMinutes());

                        if (minutesDifference <= DUPLICATE_WINDOW_MINUTES) {

                                return new RiskFinding(
                                                RiskType.DUPLICATE_TRANSACTION,
                                                25,
                                                RiskSeverity.MEDIUM,
                                                "Possible duplicate transaction detected");
                        }
                }

                return null;
        }
}