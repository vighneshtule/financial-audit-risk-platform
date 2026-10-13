package com.vighnesh.controller;

import com.vighnesh.service.AuditService;
import jakarta.validation.Valid;
import model.AuditDecision;
import model.AuditEvent;
import model.CreateAuditDecisionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/risk/transactions/{transactionId}/audit")
public class AuditController {

    private final AuditService auditService;

    @Autowired
    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Records an audit decision for the given transaction.
     * <p>
     * The actor stored in the audit trail is always taken from the authenticated
     * {@link Principal} — never from the client-supplied {@code decidedBy} field.
     * This prevents audit trail forgery by privileged users.
     */
    @PostMapping("/decisions")
    public ResponseEntity<AuditDecision> createDecision(
            @PathVariable("transactionId") String transactionId,
            @Valid @RequestBody CreateAuditDecisionRequest request,
            Principal principal) throws Exception {

        if (principal == null || principal.getName() == null || principal.getName().trim().isEmpty()) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Authenticated principal is required to record audit decision");
        }

        // Derive actor exclusively from the authenticated identity.
        String actor = principal.getName().trim();
        AuditDecision decision = auditService.recordDecision(transactionId, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(decision);
    }

    @GetMapping("/decisions")
    public ResponseEntity<List<AuditDecision>> getDecisions(
            @PathVariable("transactionId") String transactionId) throws Exception {

        return ResponseEntity.ok(auditService.getDecisionHistory(transactionId));
    }

    @GetMapping("/decision")
    public ResponseEntity<AuditDecision> getLatestDecision(
            @PathVariable("transactionId") String transactionId) throws Exception {

        AuditDecision latest = auditService.getLatestDecision(transactionId);
        return ResponseEntity.ok(latest);
    }

    @GetMapping("/events")
    public ResponseEntity<List<AuditEvent>> getAuditEvents(
            @PathVariable("transactionId") String transactionId) throws Exception {

        return ResponseEntity.ok(auditService.getAuditEventHistory(transactionId));
    }
}
