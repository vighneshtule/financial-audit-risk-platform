package model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateAuditDecisionRequest {

    private Long analysisRunId;

    @NotNull(message = "Decision is required")
    private AuditDecisionType decision;

    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;

    @NotBlank(message = "decidedBy is required")
    @Size(max = 100, message = "decidedBy must not exceed 100 characters")
    private String decidedBy;

    public CreateAuditDecisionRequest() {
    }

    public CreateAuditDecisionRequest(AuditDecisionType decision, String comment, String decidedBy) {
        this(null, decision, comment, decidedBy);
    }

    public CreateAuditDecisionRequest(Long analysisRunId, AuditDecisionType decision, String comment, String decidedBy) {
        this.analysisRunId = analysisRunId;
        this.decision = decision;
        this.comment = comment;
        this.decidedBy = decidedBy;
    }

    public Long getAnalysisRunId() {
        return analysisRunId;
    }

    public void setAnalysisRunId(Long analysisRunId) {
        this.analysisRunId = analysisRunId;
    }

    public AuditDecisionType getDecision() {
        return decision;
    }

    public void setDecision(AuditDecisionType decision) {
        this.decision = decision;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(String decidedBy) {
        this.decidedBy = decidedBy;
    }
}
