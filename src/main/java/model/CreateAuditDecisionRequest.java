package model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating an audit decision.
 *
 * <p><strong>Security note:</strong> The {@code decidedBy} field in this DTO is
 * retained for API backward compatibility (callers may still send it) but it is
 * intentionally <em>ignored</em> when recording the audit actor.
 * The actual actor is always derived from the authenticated Spring Security
 * {@link java.security.Principal} in {@link com.vighnesh.controller.AuditController},
 * preventing clients from forging the audit trail.
 */
public class CreateAuditDecisionRequest {

    private Long analysisRunId;

    @NotNull(message = "Decision is required")
    private AuditDecisionType decision;

    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;

    /**
     * Retained for API compatibility. This value is NOT used as the audit actor;
     * the authenticated principal name is used instead.
     */
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
