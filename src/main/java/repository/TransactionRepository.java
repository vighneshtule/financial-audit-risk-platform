package repository;

import config.DatabaseConnection;
import model.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransactionRepository {

        //save()

    public void save(
                Connection connection,
                Transaction transaction)
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

        try (PreparedStatement statement =
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

                statement.setBigDecimal(
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

    //findById()
    public Transaction findById(String transactionId)
        throws SQLException {

        String sql = """
                SELECT
                        transaction_id,
                        vendor,
                        employee,
                        amount,
                        transaction_time,
                        category
                FROM transactions
                WHERE transaction_id = ?
                """;

        try (Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

                statement.setString(1, transactionId);

                try (var resultSet =
                        statement.executeQuery()) {

                if (resultSet.next()) {

                        return new Transaction(
                                resultSet.getString("transaction_id"),
                                resultSet.getString("vendor"),
                                resultSet.getString("employee"),
                                resultSet.getBigDecimal("amount"),
                                resultSet.getTimestamp(
                                        "transaction_time"
                                ).toLocalDateTime(),
                                resultSet.getString("category")
                        );
                }
                }
        }

        return null;
        }

        //findall()

        public List<Transaction> findAll()
                throws SQLException {

        String sql = """
                SELECT
                        transaction_id,
                        vendor,
                        employee,
                        amount,
                        transaction_time,
                        category
                FROM transactions
                ORDER BY transaction_time
                """;

        List<Transaction> transactions =
                new ArrayList<>();

        try (Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                var resultSet =
                        statement.executeQuery()) {

                while (resultSet.next()) {

                Transaction transaction =
                        new Transaction(
                                resultSet.getString(
                                        "transaction_id"
                                ),
                                resultSet.getString(
                                        "vendor"
                                ),
                                resultSet.getString(
                                        "employee"
                                ),
                                resultSet.getBigDecimal(
                                        "amount"
                                ),
                                resultSet.getTimestamp(
                                        "transaction_time"
                                ).toLocalDateTime(),
                                resultSet.getString(
                                        "category"
                                )
                        );

                transactions.add(transaction);
                }
        }

        return transactions;
        }

        //delete()


}