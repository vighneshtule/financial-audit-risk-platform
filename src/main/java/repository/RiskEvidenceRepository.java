package repository;

import model.RelatedTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RiskEvidenceRepository {

    private final DataSource dataSource;

    @Autowired
    public RiskEvidenceRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public RiskEvidenceRepository() {
        this.dataSource = null;
    }

    private Connection openConnection() throws SQLException {
        if (dataSource != null) {
            return dataSource.getConnection();
        }
        return config.DatabaseConnection.getConnection();
    }

    public List<RelatedTransaction> findRelatedByVendor(String vendor, String excludeTransactionId) throws SQLException {
        String sql = """
            WITH latest_runs AS (
                SELECT DISTINCT ON (transaction_id)
                    transaction_id, risk_score, risk_level
                FROM risk_analysis_runs
                ORDER BY transaction_id, analyzed_at DESC, id DESC
            )
            SELECT
                t.transaction_id,
                t.amount,
                t.transaction_time,
                lr.risk_score,
                lr.risk_level
            FROM transactions t
            LEFT JOIN latest_runs lr ON t.transaction_id = lr.transaction_id
            WHERE t.vendor = ?
              AND t.transaction_id <> ?
            ORDER BY t.transaction_time
            """;
        return executeRelatedQuery(sql, vendor, excludeTransactionId);
    }

    public List<RelatedTransaction> findRelatedByEmployee(String employee, String excludeTransactionId) throws SQLException {
        String sql = """
            WITH latest_runs AS (
                SELECT DISTINCT ON (transaction_id)
                    transaction_id, risk_score, risk_level
                FROM risk_analysis_runs
                ORDER BY transaction_id, analyzed_at DESC, id DESC
            )
            SELECT
                t.transaction_id,
                t.amount,
                t.transaction_time,
                lr.risk_score,
                lr.risk_level
            FROM transactions t
            LEFT JOIN latest_runs lr ON t.transaction_id = lr.transaction_id
            WHERE t.employee = ?
              AND t.transaction_id <> ?
            ORDER BY t.transaction_time
            """;
        return executeRelatedQuery(sql, employee, excludeTransactionId);
    }

    public List<RelatedTransaction> findRelatedByCategory(String category, String excludeTransactionId) throws SQLException {
        String sql = """
            WITH latest_runs AS (
                SELECT DISTINCT ON (transaction_id)
                    transaction_id, risk_score, risk_level
                FROM risk_analysis_runs
                ORDER BY transaction_id, analyzed_at DESC, id DESC
            )
            SELECT
                t.transaction_id,
                t.amount,
                t.transaction_time,
                lr.risk_score,
                lr.risk_level
            FROM transactions t
            LEFT JOIN latest_runs lr ON t.transaction_id = lr.transaction_id
            WHERE t.category = ?
              AND t.transaction_id <> ?
            ORDER BY t.transaction_time
            """;
        return executeRelatedQuery(sql, category, excludeTransactionId);
    }

    private List<RelatedTransaction> executeRelatedQuery(String sql, String filterValue, String excludeTransactionId) throws SQLException {
        List<RelatedTransaction> list = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, filterValue);
            statement.setString(2, excludeTransactionId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String txnId = rs.getString("transaction_id");
                    var amount = rs.getBigDecimal("amount");
                    String txnTime = rs.getTimestamp("transaction_time") != null 
                        ? rs.getTimestamp("transaction_time").toLocalDateTime().toString()
                        : null;
                    
                    Integer riskScore = null;
                    int scoreVal = rs.getInt("risk_score");
                    if (!rs.wasNull()) {
                        riskScore = scoreVal;
                    }
                    
                    String riskLevel = rs.getString("risk_level");

                    list.add(new RelatedTransaction(txnId, amount, txnTime, riskScore, riskLevel));
                }
            }
        }
        return list;
    }
}
