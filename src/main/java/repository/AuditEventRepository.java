package repository;

import model.AuditEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AuditEventRepository {

    private final DataSource dataSource;

    @Autowired
    public AuditEventRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public long save(
            String transactionId,
            Long decisionId,
            String eventType,
            String eventDetails,
            String actor)
            throws SQLException {

        String sql = """
                INSERT INTO audit_events (
                    transaction_id,
                    decision_id,
                    event_type,
                    event_details,
                    actor
                )
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """;

        Connection connection = DataSourceUtils.getConnection(dataSource);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, transactionId);
            if (decisionId != null) {
                statement.setLong(2, decisionId);
            } else {
                statement.setNull(2, Types.BIGINT);
            }
            statement.setString(3, eventType);
            statement.setString(4, eventDetails);
            statement.setString(5, actor);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }
                throw new SQLException("Failed to create audit event");
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public List<AuditEvent> findByTransactionId(String transactionId) throws SQLException {
        String sql = """
                SELECT
                    id,
                    transaction_id,
                    decision_id,
                    event_type,
                    event_details,
                    actor,
                    created_at
                FROM audit_events
                WHERE transaction_id = ?
                ORDER BY created_at DESC, id DESC
                """;

        List<AuditEvent> events = new ArrayList<>();
        Connection connection = DataSourceUtils.getConnection(dataSource);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, transactionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long rawDecisionId = resultSet.getLong("decision_id");
                    Long decisionId = resultSet.wasNull() ? null : rawDecisionId;

                    events.add(new AuditEvent(
                            resultSet.getLong("id"),
                            resultSet.getString("transaction_id"),
                            decisionId,
                            resultSet.getString("event_type"),
                            resultSet.getString("event_details"),
                            resultSet.getString("actor"),
                            resultSet.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }

        return events;
    }
}
