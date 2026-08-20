package rule;

import model.Transaction;

import java.time.LocalTime;

public class UnusualTimeRule implements RiskRule {

    private static final LocalTime BUSINESS_START = LocalTime.of(9, 0);
    private static final LocalTime BUSINESS_END = LocalTime.of(18, 0);

    @Override
    public int evaluate(Transaction transaction) {

        LocalTime transactionTime =
                transaction.getTransactionTime().toLocalTime();

        if (transactionTime.isBefore(BUSINESS_START)
                || transactionTime.isAfter(BUSINESS_END)) {

            return 20;
        }

        return 0;
    }

    @Override
    public String getReason() {
        return "Transaction occurred outside normal business hours";
    }
}