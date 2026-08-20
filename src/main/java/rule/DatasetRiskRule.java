package rule;

import model.RiskFinding;
import model.Transaction;

import java.util.List;

public interface DatasetRiskRule {

    RiskFinding evaluate(
            Transaction transaction,
            List<Transaction> transactions
    );
}