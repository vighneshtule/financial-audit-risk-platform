package model;

import java.time.LocalDateTime;

public class AuditDecision {

    private final long id;
    private final String transactionId;
    private final long analysisRunId;
    private final AuditDecisionType decision;
    private final String comment;
    private final String decidedBy;
    private final LocalDateTime decidedAt;

    public AuditDecision(
            long id,
            String transactionId,
            long analysisRunId,
            AuditDecisionType decision,
            String comment,
            String decidedBy,
            LocalDateTime decidedAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.analysisRunId = analysisRunId;
        this.decision = decision;
        this.comment = comment;
        this.decidedBy = decidedBy;
        this.decidedAt = decidedAt;
    }

    public long getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public long getAnalysisRunId() {
        return analysisRunId;
    }

    public AuditDecisionType getDecision() {
        return decision;
    }

    public String getComment() {
        return comment;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }
}
