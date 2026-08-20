package rule;

import model.Transaction;

public interface RiskRule {

    int evaluate(Transaction transaction);

    String getReason();
}