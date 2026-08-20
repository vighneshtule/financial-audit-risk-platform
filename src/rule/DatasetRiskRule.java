package rule;

import model.Transaction;

import java.util.List;

public interface DatasetRiskRule {

    int evaluate(Transaction transaction, List<Transaction> transactions);

    String getReason();
}