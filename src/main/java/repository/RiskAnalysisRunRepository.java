package repository;

import model.RiskAnalysisRun;
import model.RiskSeverity;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Repository
public class RiskAnalysisRunRepository {

    private final DataSource dataSource;

    public RiskAnalysisRunRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public long save(
            String transactionId,
            int riskScore,
            RiskSeverity riskLevel)
            throws SQLException {

        String sql = """
                INSERT INTO risk_analysis_runs (
                    transaction_id,
                    risk_score,
                    risk_level
                )
                VALUES (?, ?, ?)
                RETURNING id
                """;

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, transactionId);
            statement.setInt(2, riskScore);
            statement.setString(3, riskLevel.name());

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }

                throw new SQLException(
                        "Failed to create risk analysis run"
                );
            }
        }
    }

    public RiskAnalysisRun findLatestByTransactionId(
            String transactionId)
            throws SQLException {

        String sql = """
                SELECT
                    id,
                    transaction_id,
                    risk_score,
                    risk_level,
                    analyzed_at
                FROM risk_analysis_runs
                WHERE transaction_id = ?
                ORDER BY analyzed_at DESC, id DESC
                LIMIT 1
                """;

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, transactionId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                return new RiskAnalysisRun(
                        resultSet.getLong("id"),
                        resultSet.getString("transaction_id"),
                        resultSet.getInt("risk_score"),
                        RiskSeverity.valueOf(
                                resultSet.getString("risk_level")
                        ),
                        resultSet.getTimestamp("analyzed_at")
                                .toLocalDateTime()
                );
            }
        }
    }

    public int countByTransactionId(
            String transactionId)
            throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM risk_analysis_runs
                WHERE transaction_id = ?
                """;

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    transactionId
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                resultSet.next();

                return resultSet.getInt(1);
            }
        }
    }
}
