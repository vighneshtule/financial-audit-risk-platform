package rule;

import model.Transaction;

import java.time.Duration;
import java.util.List;

public class DuplicateTransactionRule implements DatasetRiskRule {

    private static final long DUPLICATE_WINDOW_MINUTES = 10;

    @Override
    public int evaluate(
            Transaction transaction,
            List<Transaction> transactions) {

        for (Transaction other : transactions) {

            // Don't compare the transaction with itself
            if (transaction.getId().equals(other.getId())) {
                continue;
            }

            boolean sameVendor =
                    transaction.getVendor()
                            .equalsIgnoreCase(other.getVendor());

            boolean sameEmployee =
                    transaction.getEmployee()
                            .equalsIgnoreCase(other.getEmployee());

            boolean sameAmount =
                    transaction.getAmount() == other.getAmount();

            boolean sameCategory =
                    transaction.getCategory()
                            .equalsIgnoreCase(other.getCategory());

            long minutesDifference = Math.abs(
                    Duration.between(
                            transaction.getTransactionTime(),
                            other.getTransactionTime()
                    ).toMinutes()
            );

            boolean withinTimeWindow =
                    minutesDifference <= DUPLICATE_WINDOW_MINUTES;

            if (sameVendor
                    && sameEmployee
                    && sameAmount
                    && sameCategory
                    && withinTimeWindow) {

                return 25;
            }
        }

        return 0;
    }

    @Override
    public String getReason() {
        return "Possible duplicate transaction detected";
    }
}