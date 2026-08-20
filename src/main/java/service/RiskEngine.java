package service;

import model.RiskFinding;
import model.RiskReport;
import model.RiskSeverity;
import model.Transaction;
import rule.DatasetRiskRule;
import rule.RiskRule;


import java.util.ArrayList;
import java.util.List;

public class RiskEngine {

    private final List<RiskRule> rules = new ArrayList<>();

    private final List<DatasetRiskRule> datasetRules =
            new ArrayList<>();

    public void addRule(RiskRule rule) {
        rules.add(rule);
    }

    public void addDatasetRule(DatasetRiskRule rule) {
        datasetRules.add(rule);
    }

    public RiskReport analyze(
            Transaction transaction,
            List<Transaction> transactions) {

        int totalRisk = 0;

        List<RiskFinding> findings = new ArrayList<>();

        for (RiskRule rule : rules) {

            RiskFinding finding =
                    rule.evaluate(transaction);

            if (finding != null) {

                totalRisk += finding.getScore();

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

                totalRisk += finding.getScore();

                findings.add(finding);
            }
        }

        totalRisk = Math.min(totalRisk, 100);

        RiskSeverity severity;

        if (totalRisk >= 80) {
            severity = RiskSeverity.CRITICAL;
        } else if (totalRisk >= 60) {
            severity = RiskSeverity.HIGH;
        } else if (totalRisk >= 30) {
            severity = RiskSeverity.MEDIUM;
        } else {
            severity = RiskSeverity.LOW;
        }

        return new RiskReport(
                totalRisk,
                severity,
                findings
        );
    }
}