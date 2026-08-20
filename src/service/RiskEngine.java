package service;

import model.Transaction;
import rule.RiskRule;

import java.util.ArrayList;
import java.util.List;

public class RiskEngine {

    private final List<RiskRule> rules = new ArrayList<>();

    public void addRule(RiskRule rule) {
        rules.add(rule);
    }

    public int calculateRisk(Transaction transaction) {

        int totalRisk = 0;

        for (RiskRule rule : rules) {
            totalRisk += rule.evaluate(transaction);
        }

        return Math.min(totalRisk, 100);
    }
}