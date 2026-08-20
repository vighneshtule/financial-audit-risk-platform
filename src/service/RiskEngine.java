package service;

import model.RiskReport;
import model.Transaction;
import rule.RiskRule;

import java.util.ArrayList;
import java.util.List;

public class RiskEngine {

    private final List<RiskRule> rules = new ArrayList<>();

    public void addRule(RiskRule rule) {
        rules.add(rule);
    }

    public RiskReport analyze(Transaction transaction) {

        int totalRisk = 0;

        List<String> reasons = new ArrayList<>();

        for (RiskRule rule : rules) {

            int score = rule.evaluate(transaction);

            if (score > 0) {
                totalRisk += score;
                reasons.add(rule.getReason());
            }
        }

        totalRisk = Math.min(totalRisk, 100);

        return new RiskReport(totalRisk, reasons);
    }
}