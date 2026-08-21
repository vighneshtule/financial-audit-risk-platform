package model;

import java.math.BigDecimal;
import java.util.Objects;

public class TransactionKey {

    private final String vendor;
    private final String employee;
    private final BigDecimal amount;
    private final String category;

    public TransactionKey(
            String vendor,
            String employee,
            BigDecimal amount,
            String category) {

        this.vendor = vendor.toLowerCase();
        this.employee = employee.toLowerCase();
        this.amount = amount;
        this.category = category.toLowerCase();
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if (!(o instanceof TransactionKey other))
            return false;

        return vendor.equals(other.vendor)
                && employee.equals(other.employee)
                && amount.compareTo(other.amount) == 0
                && category.equals(other.category);
    }

    @Override
    public int hashCode() {

        return Objects.hash(
                vendor,
                employee,
                amount.stripTrailingZeros(),
                category
        );
    }
}