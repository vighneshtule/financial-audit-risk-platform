package service;

import com.vighnesh.service.RiskScoreCalculator;
import model.RiskFinding;
import model.RiskReport;
import model.RiskSeverity;
import model.Transaction;
import rule.DatasetRiskRule;
import rule.RiskRule;

import java.util.ArrayList;
import java.util.List;

public class RiskEngine {

    private final RiskScoreCalculator scoreCalculator;

    private final List<RiskRule> rules = new ArrayList<>();

    private final List<DatasetRiskRule> datasetRules =
            new ArrayList<>();

    public RiskEngine() {
        this(new RiskScoreCalculator());
    }

    public RiskEngine(RiskScoreCalculator scoreCalculator) {
        this.scoreCalculator = scoreCalculator;
    }

    public void addRule(RiskRule rule) {
        rules.add(rule);
    }

    public void addDatasetRule(DatasetRiskRule rule) {
        datasetRules.add(rule);
    }

    public RiskReport analyze(
            Transaction transaction,
            List<Transaction> transactions) {

        List<RiskFinding> findings = new ArrayList<>();

        for (RiskRule rule : rules) {

            RiskFinding finding =
                    rule.evaluate(transaction);

            if (finding != null) {
                findings.add(finding);
            }
        }

        for (DatasetRiskRule rule : datasetRules) {

            RiskFinding finding =
                    rule.evaluate(
                            transaction,
                            transactions
                    );

            if (finding != null) {
                findings.add(finding);
            }
        }

        int totalRisk = scoreCalculator.calculateScore(findings);

        RiskSeverity severity = scoreCalculator.determineSeverity(totalRisk);

        return new RiskReport(
                totalRisk,
                severity,
                findings
        );
    }
}