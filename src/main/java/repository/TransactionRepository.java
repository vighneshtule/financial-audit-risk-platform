package repository;

import config.DatabaseConnection;
import model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TransactionRepository {

    private final DataSource dataSource;

    // Spring-managed path: DataSource is injected by Spring,
    // so @DynamicPropertySource in integration tests takes effect.
    @Autowired
    public TransactionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Non-Spring path: used by plain unit tests (e.g.
    // TransactionImportServiceTest) that do new TransactionRepository().
    // Falls back to DatabaseConnection / System.getenv credentials.
    public TransactionRepository() {
        this.dataSource = null;
    }

    // Single place that decides which connection to open.
    private Connection openConnection() throws SQLException {
        if (dataSource != null) {
            return dataSource.getConnection();
        }
        return DatabaseConnection.getConnection();
    }

    // save() already receives a caller-managed Connection (for
    // transactional CSV import), so it does NOT call openConnection().
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

            statement.setString(1, transaction.getId());
            statement.setString(2, transaction.getVendor());
            statement.setString(3, transaction.getEmployee());
            statement.setBigDecimal(4, transaction.getAmount());
            statement.setObject(5, transaction.getTransactionTime());
            statement.setString(6, transaction.getCategory());

            statement.executeUpdate();
        }
    }

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

        try (Connection connection = openConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, transactionId);

            try (var resultSet = statement.executeQuery()) {

                if (resultSet.next()) {

                    return new Transaction(
                            resultSet.getString("transaction_id"),
                            resultSet.getString("vendor"),
                            resultSet.getString("employee"),
                            resultSet.getBigDecimal("amount"),
                            resultSet.getTimestamp("transaction_time")
                                    .toLocalDateTime(),
                            resultSet.getString("category")
                    );
                }
            }
        }

        return null;
    }

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

        List<Transaction> transactions = new ArrayList<>();

        try (Connection connection = openConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {

            while (resultSet.next()) {

                transactions.add(new Transaction(
                        resultSet.getString("transaction_id"),
                        resultSet.getString("vendor"),
                        resultSet.getString("employee"),
                        resultSet.getBigDecimal("amount"),
                        resultSet.getTimestamp("transaction_time")
                                .toLocalDateTime(),
                        resultSet.getString("category")
                ));
            }
        }

        return transactions;
    }
}
