package rule;

import model.RiskFinding;
import model.RiskSeverity;
import model.RiskType;
import model.Transaction;

public class HighAmountRule implements RiskRule {

    private static final double THRESHOLD = 100_000;

    @Override
    public RiskFinding evaluate(Transaction transaction) {

        if (transaction.getAmount() > THRESHOLD) {

            return new RiskFinding(
                    RiskType.HIGH_AMOUNT,
                    30,
                    RiskSeverity.MEDIUM,
                    "Transaction amount exceeds ₹1,00,000"
            );
        }

        return null;
    }
}