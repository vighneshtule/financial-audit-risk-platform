package model;

import java.time.LocalDateTime;

public class AuditEvent {

    private final long id;
    private final String transactionId;
    private final Long decisionId;
    private final String eventType;
    private final String eventDetails;
    private final String actor;
    private final LocalDateTime createdAt;

    public AuditEvent(
            long id,
            String transactionId,
            Long decisionId,
            String eventType,
            String eventDetails,
            String actor,
            LocalDateTime createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.decisionId = decisionId;
        this.eventType = eventType;
        this.eventDetails = eventDetails;
        this.actor = actor;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Long getDecisionId() {
        return decisionId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventDetails() {
        return eventDetails;
    }

    public String getActor() {
        return actor;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
