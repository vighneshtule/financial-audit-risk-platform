package model;

import java.time.LocalDateTime;

public class Transaction {

    private final String id;
    private final String vendor;
    private final String employee;
    private final double amount;
    private final LocalDateTime transactionTime;
    private final String category;

    public Transaction(
            String id,
            String vendor,
            String employee,
            double amount,
            LocalDateTime transactionTime,
            String category) {

        this.id = id;
        this.vendor = vendor;
        this.employee = employee;
        this.amount = amount;
        this.transactionTime = transactionTime;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public String getVendor() {
        return vendor;
    }

    public String getEmployee() {
        return employee;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTransactionTime() {
        return transactionTime;
    }

    public String getCategory() {
        return category;
    }
}