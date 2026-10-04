package com.vighnesh.service;

import com.vighnesh.exception.AnalysisRunNotFoundException;
import com.vighnesh.exception.TransactionNotFoundException;
import model.AuditDecision;
import model.AuditDecisionType;
import model.AuditEvent;
import model.CreateAuditDecisionRequest;
import model.RiskAnalysisRun;
import model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.AuditDecisionRepository;
import repository.AuditEventRepository;
import repository.RiskAnalysisRunRepository;
import repository.TransactionRepository;

import java.util.List;

@Service
public class AuditService {

    public static final String EVENT_TYPE_AUDIT_DECISION_CREATED = "AUDIT_DECISION_CREATED";

    private final TransactionRepository transactionRepository;
    private final RiskAnalysisRunRepository riskAnalysisRunRepository;
    private final AuditDecisionRepository auditDecisionRepository;
    private final AuditEventRepository auditEventRepository;

    @Autowired
    public AuditService(
            TransactionRepository transactionRepository,
            RiskAnalysisRunRepository riskAnalysisRunRepository,
            AuditDecisionRepository auditDecisionRepository,
            AuditEventRepository auditEventRepository) {
        this.transactionRepository = transactionRepository;
        this.riskAnalysisRunRepository = riskAnalysisRunRepository;
        this.auditDecisionRepository = auditDecisionRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public AuditDecision recordDecision(
            String transactionId,
            CreateAuditDecisionRequest request) throws Exception {

        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Audit decision request cannot be null");
        }
        if (request.getDecision() == null) {
            throw new IllegalArgumentException("Decision is required");
        }
        if (request.getDecidedBy() == null || request.getDecidedBy().trim().isEmpty()) {
            throw new IllegalArgumentException("decidedBy is required");
        }

        // 1. Validate transaction exists
        Transaction transaction = transactionRepository.findById(transactionId);
        if (transaction == null) {
            throw new TransactionNotFoundException("Transaction not found: " + transactionId);
        }

        // 2. Validate analysisRunId belongs to this transaction
        Long targetAnalysisRunId = request.getAnalysisRunId();
        if (targetAnalysisRunId == null) {
            // Default to latest analysis run for this transaction
            RiskAnalysisRun latestRun = riskAnalysisRunRepository.findLatestByTransactionId(transactionId);
            if (latestRun == null) {
                throw new AnalysisRunNotFoundException(
                        "No risk analysis run found for transaction " + transactionId + " to attach an audit decision");
            }
            targetAnalysisRunId = latestRun.getId();
        } else {
            RiskAnalysisRun run = riskAnalysisRunRepository.findByIdAndTransactionId(targetAnalysisRunId, transactionId);
            if (run == null) {
                throw new AnalysisRunNotFoundException(
                        "Analysis run " + targetAnalysisRunId + " does not belong to transaction " + transactionId);
            }
        }

        // 3. Create audit decision
        long decisionId = auditDecisionRepository.save(
                transactionId,
                targetAnalysisRunId,
                request.getDecision(),
                request.getComment(),
                request.getDecidedBy().trim()
        );

        // 4. Create audit event for that decision
        String eventDetails = String.format(
                "Audit decision %s recorded for analysis run %d",
                request.getDecision().name(),
                targetAnalysisRunId
        );

        auditEventRepository.save(
                transactionId,
                decisionId,
                EVENT_TYPE_AUDIT_DECISION_CREATED,
                eventDetails,
                request.getDecidedBy().trim()
        );

        // 5. Return the created decision
        return auditDecisionRepository.findById(decisionId);
    }

    public AuditDecision getLatestDecision(String transactionId) throws Exception {
        Transaction transaction = transactionRepository.findById(transactionId);
        if (transaction == null) {
            throw new TransactionNotFoundException("Transaction not found: " + transactionId);
        }
        return auditDecisionRepository.findLatestByTransactionId(transactionId);
    }

    public List<AuditDecision> getDecisionHistory(String transactionId) throws Exception {
        Transaction transaction = transactionRepository.findById(transactionId);
        if (transaction == null) {
            throw new TransactionNotFoundException("Transaction not found: " + transactionId);
        }
        return auditDecisionRepository.findByTransactionId(transactionId);
    }

    public List<AuditEvent> getAuditEventHistory(String transactionId) throws Exception {
        Transaction transaction = transactionRepository.findById(transactionId);
        if (transaction == null) {
            throw new TransactionNotFoundException("Transaction not found: " + transactionId);
        }
        return auditEventRepository.findByTransactionId(transactionId);
    }
}
