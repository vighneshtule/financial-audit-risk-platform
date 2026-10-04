package repository;

import model.AuditDecision;
import model.AuditDecisionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AuditDecisionRepository {

    private final DataSource dataSource;

    @Autowired
    public AuditDecisionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public long save(
            String transactionId,
            long analysisRunId,
            AuditDecisionType decision,
            String comment,
            String decidedBy)
            throws SQLException {

        String sql = """
                INSERT INTO audit_decisions (
                    transaction_id,
                    analysis_run_id,
                    decision,
                    comment,
                    decided_by
                )
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """;

        Connection connection = DataSourceUtils.getConnection(dataSource);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, transactionId);
            statement.setLong(2, analysisRunId);
            statement.setString(3, decision.name());
            statement.setString(4, comment);
            statement.setString(5, decidedBy);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }
                throw new SQLException("Failed to create audit decision");
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public AuditDecision findLatestByTransactionId(String transactionId) throws SQLException {
        String sql = """
                SELECT
                    id,
                    transaction_id,
                    analysis_run_id,
                    decision,
                    comment,
                    decided_by,
                    decided_at
                FROM audit_decisions
                WHERE transaction_id = ?
                ORDER BY decided_at DESC, id DESC
                LIMIT 1
                """;

        Connection connection = DataSourceUtils.getConnection(dataSource);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, transactionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapResultSetToAuditDecision(resultSet);
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public List<AuditDecision> findByTransactionId(String transactionId) throws SQLException {
        String sql = """
                SELECT
                    id,
                    transaction_id,
                    analysis_run_id,
                    decision,
                    comment,
                    decided_by,
                    decided_at
                FROM audit_decisions
                WHERE transaction_id = ?
                ORDER BY decided_at DESC, id DESC
                """;

        List<AuditDecision> decisions = new ArrayList<>();
        Connection connection = DataSourceUtils.getConnection(dataSource);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, transactionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    decisions.add(mapResultSetToAuditDecision(resultSet));
                }
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }

        return decisions;
    }

    public AuditDecision findById(long id) throws SQLException {
        String sql = """
                SELECT
                    id,
                    transaction_id,
                    analysis_run_id,
                    decision,
                    comment,
                    decided_by,
                    decided_at
                FROM audit_decisions
                WHERE id = ?
                """;

        Connection connection = DataSourceUtils.getConnection(dataSource);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapResultSetToAuditDecision(resultSet);
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private AuditDecision mapResultSetToAuditDecision(ResultSet rs) throws SQLException {
        return new AuditDecision(
                rs.getLong("id"),
                rs.getString("transaction_id"),
                rs.getLong("analysis_run_id"),
                AuditDecisionType.valueOf(rs.getString("decision")),
                rs.getString("comment"),
                rs.getString("decided_by"),
                rs.getTimestamp("decided_at").toLocalDateTime()
        );
    }
}
