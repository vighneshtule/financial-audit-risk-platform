package rule;

import model.Transaction;

public class HighAmountRule implements RiskRule {

    private static final double THRESHOLD = 100_000;

    @Override
    public int evaluate(Transaction transaction) {

        if (transaction.getAmount() > THRESHOLD) {
            return 30;
        }

        return 0;
    }

    @Override
    public String getReason() {
        return "Transaction amount exceeds ₹1,00,000";
    }
}