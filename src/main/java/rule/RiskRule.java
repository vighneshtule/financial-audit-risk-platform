package rule;

import model.RiskFinding;
import model.Transaction;

public interface RiskRule {

    RiskFinding evaluate(Transaction transaction);
}