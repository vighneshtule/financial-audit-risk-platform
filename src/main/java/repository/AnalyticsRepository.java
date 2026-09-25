package repository;

import model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AnalyticsRepository {

    private final DataSource dataSource;

    @Autowired
    public AnalyticsRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DatasetOverview getDatasetOverview() throws SQLException {
        String sql = """
                WITH latest_runs AS (
                    SELECT DISTINCT ON (transaction_id)
                        id, transaction_id, risk_score, risk_level, analyzed_at
                    FROM risk_analysis_runs
                    ORDER BY transaction_id, analyzed_at DESC, id DESC
                )
                SELECT
                    COUNT(t.transaction_id) AS total_transactions,
                    COUNT(lr.id) AS analyzed_transactions,
                    COALESCE(SUM(t.amount), 0) AS total_audit_value,
                    COALESCE(MAX(lr.risk_score), 0) AS highest_risk_score,
                    COUNT(CASE WHEN lr.risk_level = 'LOW' THEN 1 END) AS low_risk,
                    COUNT(CASE WHEN lr.risk_level = 'MEDIUM' THEN 1 END) AS medium_risk,
                    COUNT(CASE WHEN lr.risk_level = 'HIGH' THEN 1 END) AS high_risk,
                    COUNT(CASE WHEN lr.risk_level = 'CRITICAL' THEN 1 END) AS critical_risk,
                    (SELECT COUNT(*) FROM risk_findings rf JOIN latest_runs lr2 ON rf.analysis_run_id = lr2.id) AS total_findings
                FROM transactions t
                LEFT JOIN latest_runs lr ON t.transaction_id = lr.transaction_id
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            if (rs.next()) {
                return new DatasetOverview(
                        rs.getInt("total_transactions"),
                        rs.getInt("analyzed_transactions"),
                        rs.getBigDecimal("total_audit_value"),
                        rs.getInt("total_findings"),
                        rs.getInt("low_risk"),
                        rs.getInt("medium_risk"),
                        rs.getInt("high_risk"),
                        rs.getInt("critical_risk"),
                        rs.getInt("highest_risk_score")
                );
            }

            return new DatasetOverview(0, 0, BigDecimal.ZERO, 0, 0, 0, 0, 0, 0);
        }
    }

    public List<SeverityCount> getSeverityDistribution() throws SQLException {
        String sql = """
                WITH latest_runs AS (
                    SELECT DISTINCT ON (transaction_id)
                        id, transaction_id, risk_score, risk_level, analyzed_at
                    FROM risk_analysis_runs
                    ORDER BY transaction_id, analyzed_at DESC, id DESC
                )
                SELECT
                    s.severity,
                    COUNT(lr.id) AS count
                FROM (VALUES ('LOW'), ('MEDIUM'), ('HIGH'), ('CRITICAL')) AS s(severity)
                LEFT JOIN latest_runs lr ON lr.risk_level = s.severity
                GROUP BY s.severity
                ORDER BY
                    CASE s.severity
                        WHEN 'LOW' THEN 1
                        WHEN 'MEDIUM' THEN 2
                        WHEN 'HIGH' THEN 3
                        WHEN 'CRITICAL' THEN 4
                    END
                """;

        List<SeverityCount> list = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                list.add(new SeverityCount(
                        rs.getString("severity"),
                        rs.getInt("count")
                ));
            }
        }
        return list;
    }

    public List<VendorRisk> getVendorRisk() throws SQLException {
        String sql = """
                WITH latest_runs AS (
                    SELECT DISTINCT ON (transaction_id)
                        id, transaction_id, risk_score, risk_level, analyzed_at
                    FROM risk_analysis_runs
                    ORDER BY transaction_id, analyzed_at DESC, id DESC
                ),
                finding_counts AS (
                    SELECT
                        analysis_run_id,
                        COUNT(*) AS finding_count
                    FROM risk_findings
                    GROUP BY analysis_run_id
                )
                SELECT
                    t.vendor,
                    COUNT(t.transaction_id) AS transaction_count,
                    COUNT(CASE WHEN lr.risk_score > 0 THEN 1 END) AS flagged_transaction_count,
                    COALESCE(SUM(t.amount), 0) AS total_amount,
                    COALESCE(MAX(lr.risk_score), 0) AS highest_risk_score,
                    COALESCE(SUM(fc.finding_count), 0) AS finding_count
                FROM transactions t
                LEFT JOIN latest_runs lr ON t.transaction_id = lr.transaction_id
                LEFT JOIN finding_counts fc ON fc.analysis_run_id = lr.id
                GROUP BY t.vendor
                ORDER BY flagged_transaction_count DESC, finding_count DESC, highest_risk_score DESC, t.vendor ASC
                """;

        List<VendorRisk> list = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                list.add(new VendorRisk(
                        rs.getString("vendor"),
                        rs.getInt("transaction_count"),
                        rs.getInt("flagged_transaction_count"),
                        rs.getBigDecimal("total_amount"),
                        rs.getInt("highest_risk_score"),
                        rs.getInt("finding_count")
                ));
            }
        }
        return list;
    }

    public List<EmployeeRisk> getEmployeeRisk() throws SQLException {
        String sql = """
                WITH latest_runs AS (
                    SELECT DISTINCT ON (transaction_id)
                        id, transaction_id, risk_score, risk_level, analyzed_at
                    FROM risk_analysis_runs
                    ORDER BY transaction_id, analyzed_at DESC, id DESC
                ),
                finding_counts AS (
                    SELECT
                        analysis_run_id,
                        COUNT(*) AS finding_count
                    FROM risk_findings
                    GROUP BY analysis_run_id
                )
                SELECT
                    t.employee,
                    COUNT(t.transaction_id) AS transaction_count,
                    COUNT(CASE WHEN lr.risk_score > 0 THEN 1 END) AS flagged_transaction_count,
                    COALESCE(SUM(t.amount), 0) AS total_amount,
                    COALESCE(MAX(lr.risk_score), 0) AS highest_risk_score,
                    COALESCE(SUM(fc.finding_count), 0) AS finding_count
                FROM transactions t
                LEFT JOIN latest_runs lr ON t.transaction_id = lr.transaction_id
                LEFT JOIN finding_counts fc ON fc.analysis_run_id = lr.id
                GROUP BY t.employee
                ORDER BY flagged_transaction_count DESC, finding_count DESC, highest_risk_score DESC, t.employee ASC
                """;

        List<EmployeeRisk> list = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                list.add(new EmployeeRisk(
                        rs.getString("employee"),
                        rs.getInt("transaction_count"),
                        rs.getInt("flagged_transaction_count"),
                        rs.getBigDecimal("total_amount"),
                        rs.getInt("highest_risk_score"),
                        rs.getInt("finding_count")
                ));
            }
        }
        return list;
    }

    public List<RiskTypeDistribution> getRiskTypeDistribution() throws SQLException {
        String sql = """
                WITH latest_runs AS (
                    SELECT DISTINCT ON (transaction_id)
                        id, transaction_id, risk_score, risk_level, analyzed_at
                    FROM risk_analysis_runs
                    ORDER BY transaction_id, analyzed_at DESC, id DESC
                )
                SELECT
                    rf.risk_type,
                    COUNT(rf.id) AS finding_count,
                    COALESCE(SUM(rf.score), 0) AS total_score_contribution
                FROM risk_findings rf
                JOIN latest_runs lr ON rf.analysis_run_id = lr.id
                GROUP BY rf.risk_type
                ORDER BY finding_count DESC, total_score_contribution DESC, rf.risk_type ASC
                """;

        List<RiskTypeDistribution> list = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                list.add(new RiskTypeDistribution(
                        rs.getString("risk_type"),
                        rs.getInt("finding_count"),
                        rs.getInt("total_score_contribution")
                ));
            }
        }
        return list;
    }

    public List<CategoryRisk> getCategoryRisk() throws SQLException {
        String sql = """
                WITH latest_runs AS (
                    SELECT DISTINCT ON (transaction_id)
                        id, transaction_id, risk_score, risk_level, analyzed_at
                    FROM risk_analysis_runs
                    ORDER BY transaction_id, analyzed_at DESC, id DESC
                ),
                finding_counts AS (
                    SELECT
                        analysis_run_id,
                        COUNT(*) AS finding_count
                    FROM risk_findings
                    GROUP BY analysis_run_id
                )
                SELECT
                    t.category,
                    COUNT(t.transaction_id) AS transaction_count,
                    COUNT(CASE WHEN lr.risk_score > 0 THEN 1 END) AS flagged_transaction_count,
                    COALESCE(SUM(t.amount), 0) AS total_amount,
                    COALESCE(SUM(fc.finding_count), 0) AS finding_count
                FROM transactions t
                LEFT JOIN latest_runs lr ON t.transaction_id = lr.transaction_id
                LEFT JOIN finding_counts fc ON fc.analysis_run_id = lr.id
                GROUP BY t.category
                ORDER BY flagged_transaction_count DESC, finding_count DESC, total_amount DESC, t.category ASC
                """;

        List<CategoryRisk> list = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                list.add(new CategoryRisk(
                        rs.getString("category"),
                        rs.getInt("transaction_count"),
                        rs.getInt("flagged_transaction_count"),
                        rs.getBigDecimal("total_amount"),
                        rs.getInt("finding_count")
                ));
            }
        }
        return list;
    }
}
