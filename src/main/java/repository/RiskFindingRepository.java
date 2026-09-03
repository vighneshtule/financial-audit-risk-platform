package repository;

import model.RiskFinding;
import model.RiskSeverity;
import model.RiskType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RiskFindingRepository {

    private final DataSource dataSource;

    @Autowired
    public RiskFindingRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(
            long analysisRunId,
            String transactionId,
            RiskFinding finding)
            throws SQLException {

        String sql = """
                INSERT INTO risk_findings (
                    analysis_run_id,
                    transaction_id,
                    risk_type,
                    score,
                    severity,
                    explanation
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        Connection connection =
                DataSourceUtils.getConnection(dataSource);

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, analysisRunId);
            statement.setString(2, transactionId);
            statement.setString(3, finding.getType().name());
            statement.setInt(4, finding.getScore());
            statement.setString(5, finding.getSeverity().name());
            statement.setString(6, finding.getExplanation());

            statement.executeUpdate();

        } finally {
            DataSourceUtils.releaseConnection(
                    connection,
                    dataSource
            );
        }
    }

    public List<RiskFinding> findByTransactionId(
            String transactionId)
            throws SQLException {

        String sql = """
                SELECT
                    risk_type,
                    score,
                    severity,
                    explanation
                FROM risk_findings
                WHERE transaction_id = ?
                ORDER BY id
                """;

        List<RiskFinding> findings =
                new ArrayList<>();

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

            try (var resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {

                    findings.add(
                            new RiskFinding(
                                    RiskType.valueOf(
                                            resultSet.getString(
                                                    "risk_type"
                                            )
                                    ),
                                    resultSet.getInt(
                                            "score"
                                    ),
                                    RiskSeverity.valueOf(
                                            resultSet.getString(
                                                    "severity"
                                            )
                                    ),
                                    resultSet.getString(
                                            "explanation"
                                    )
                            )
                    );
                }
            }
        }

        return findings;
    }

    public List<RiskFinding> findByAnalysisRunId(
            long analysisRunId)
            throws SQLException {

        String sql = """
                SELECT
                    risk_type,
                    score,
                    severity,
                    explanation
                FROM risk_findings
                WHERE analysis_run_id = ?
                ORDER BY id
                """;

        List<RiskFinding> findings =
                new ArrayList<>();

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    analysisRunId
            );

            try (var resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {

                    findings.add(
                            new RiskFinding(
                                    RiskType.valueOf(
                                            resultSet.getString(
                                                    "risk_type"
                                            )
                                    ),
                                    resultSet.getInt(
                                            "score"
                                    ),
                                    RiskSeverity.valueOf(
                                            resultSet.getString(
                                                    "severity"
                                            )
                                    ),
                                    resultSet.getString(
                                            "explanation"
                                    )
                            )
                    );
                }
            }
        }

        return findings;
    }

    public int countByTransactionId(
            String transactionId)
            throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM risk_findings
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

            try (var resultSet =
                         statement.executeQuery()) {

                resultSet.next();

                return resultSet.getInt(1);
            }
        }
    }

    public void deleteByTransactionId(
            String transactionId)
            throws SQLException {

        String sql = """
                DELETE FROM risk_findings
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

            statement.executeUpdate();
        }
    }
}