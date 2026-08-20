package rule;

import model.RiskFinding;
import model.RiskSeverity;
import model.RiskType;
import model.Transaction;

import java.time.LocalTime;

public class UnusualTimeRule implements RiskRule {

    private static final LocalTime BUSINESS_START =
            LocalTime.of(9, 0);

    private static final LocalTime BUSINESS_END =
            LocalTime.of(18, 0);

    @Override
    public RiskFinding evaluate(Transaction transaction) {

        LocalTime transactionTime =
                transaction.getTransactionTime().toLocalTime();

        if (transactionTime.isBefore(BUSINESS_START)
                || transactionTime.isAfter(BUSINESS_END)) {

            return new RiskFinding(
                    RiskType.UNUSUAL_TRANSACTION_TIME,
                    20,
                    RiskSeverity.MEDIUM,
                    "Transaction occurred outside normal business hours"
            );
        }

        return null;
    }
}