package model;

public class RiskTypeDistribution {

    private final String riskType;
    private final int findingCount;
    private final int totalScoreContribution;

    public RiskTypeDistribution(
            String riskType,
            int findingCount,
            int totalScoreContribution) {

        this.riskType = riskType;
        this.findingCount = findingCount;
        this.totalScoreContribution = totalScoreContribution;
    }

    public String getRiskType() {
        return riskType;
    }

    public int getFindingCount() {
        return findingCount;
    }

    public int getTotalScoreContribution() {
        return totalScoreContribution;
    }
}
