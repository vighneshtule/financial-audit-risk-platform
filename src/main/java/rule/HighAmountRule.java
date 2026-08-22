package rule;

import model.RiskFinding;
import model.RiskSeverity;
import model.RiskType;
import model.Transaction;

import java.math.BigDecimal;

public class HighAmountRule implements RiskRule {

    private static final BigDecimal THRESHOLD =
            new BigDecimal("100000");

    @Override
    public RiskFinding evaluate(Transaction transaction) {

        if (transaction.getAmount().compareTo(THRESHOLD) > 0) {

            return new RiskFinding(
                    RiskType.HIGH_AMOUNT,
                    30,
                    RiskSeverity.MEDIUM,
                    "Transaction amount exceeds INR 1,00,000"
            );
        }

        return null;
    }
}