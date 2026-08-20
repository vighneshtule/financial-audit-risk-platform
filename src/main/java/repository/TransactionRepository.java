package repository;

import config.DatabaseConnection;
import model.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TransactionRepository {

    public void save(Transaction transaction)
            throws SQLException {

        String sql = """
                INSERT INTO transactions (
                    transaction_id,
                    vendor,
                    employee,
                    amount,
                    transaction_time,
                    category
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    transaction.getId()
            );

            statement.setString(
                    2,
                    transaction.getVendor()
            );

            statement.setString(
                    3,
                    transaction.getEmployee()
            );

            statement.setDouble(
                    4,
                    transaction.getAmount()
            );

            statement.setObject(
                    5,
                    transaction.getTransactionTime()
            );

            statement.setString(
                    6,
                    transaction.getCategory()
            );

            statement.executeUpdate();
        }
    }
}