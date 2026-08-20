package model;

public class RiskFinding {

    private final RiskType type;
    private final int score;
    private final RiskSeverity severity;
    private final String explanation;

    public RiskFinding(
            RiskType type,
            int score,
            RiskSeverity severity,
            String explanation) {

        this.type = type;
        this.score = score;
        this.severity = severity;
        this.explanation = explanation;
    }

    public RiskType getType() {
        return type;
    }

    public int getScore() {
        return score;
    }

    public RiskSeverity getSeverity() {
        return severity;
    }

    public String getExplanation() {
        return explanation;
    }
}