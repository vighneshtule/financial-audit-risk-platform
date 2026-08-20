package model;

import java.util.List;

public class RiskReport {

    private final int riskScore;
    private final List<String> reasons;

    public RiskReport(int riskScore, List<String> reasons) {
        this.riskScore = riskScore;
        this.reasons = reasons;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public List<String> getReasons() {
        return reasons;
    }
}