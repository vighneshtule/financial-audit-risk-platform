package model;

import java.util.Objects;

public class TransactionKey {

    private final String vendor;
    private final String employee;
    private final double amount;
    private final String category;

    public TransactionKey(
            String vendor,
            String employee,
            double amount,
            String category) {

        this.vendor = vendor.toLowerCase();
        this.employee = employee.toLowerCase();
        this.amount = amount;
        this.category = category.toLowerCase();
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof TransactionKey)) {
            return false;
        }

        TransactionKey other = (TransactionKey) obj;

        return Double.compare(amount, other.amount) == 0
                && vendor.equals(other.vendor)
                && employee.equals(other.employee)
                && category.equals(other.category);
    }

    @Override
    public int hashCode() {

        return Objects.hash(
                vendor,
                employee,
                amount,
                category
        );
    }
}