package rule;

import model.Transaction;
import model.TransactionKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DuplicateTransactionIndex {

    private final Map<TransactionKey, List<Transaction>> groups;

    public DuplicateTransactionIndex(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            this.groups = Collections.emptyMap();
            return;
        }

        Map<TransactionKey, List<Transaction>> map = new HashMap<>();

        for (Transaction current : transactions) {
            TransactionKey key = new TransactionKey(
                    current.getVendor(),
                    current.getEmployee(),
                    current.getAmount(),
                    current.getCategory()
            );

            map.computeIfAbsent(key, k -> new ArrayList<>()).add(current);
        }

        this.groups = map;
    }

    public static DuplicateTransactionIndex from(List<Transaction> transactions) {
        return new DuplicateTransactionIndex(transactions);
    }

    public List<Transaction> getPossibleDuplicates(Transaction transaction) {
        if (transaction == null || groups.isEmpty()) {
            return Collections.emptyList();
        }

        TransactionKey targetKey = new TransactionKey(
                transaction.getVendor(),
                transaction.getEmployee(),
                transaction.getAmount(),
                transaction.getCategory()
        );

        return groups.getOrDefault(targetKey, Collections.emptyList());
    }
}
