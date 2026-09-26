package com.vighnesh.service;

import com.vighnesh.exception.TransactionNotFoundException;
import model.EvidenceSummary;
import model.RelatedGroup;
import model.RelatedTransaction;
import model.Transaction;
import model.TransactionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.RiskEvidenceRepository;
import repository.TransactionRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RiskEvidenceService {

    private final TransactionRepository transactionRepository;
    private final RiskEvidenceRepository riskEvidenceRepository;

    @Autowired
    public RiskEvidenceService(TransactionRepository transactionRepository,
                               RiskEvidenceRepository riskEvidenceRepository) {
        this.transactionRepository = transactionRepository;
        this.riskEvidenceRepository = riskEvidenceRepository;
    }

    public TransactionContext getContext(String transactionId) throws Exception {
        Transaction transaction = transactionRepository.findById(transactionId);
        if (transaction == null) {
            throw new TransactionNotFoundException("Transaction not found with ID: " + transactionId);
        }

        List<RelatedTransaction> vendorTxns = riskEvidenceRepository.findRelatedByVendor(
                transaction.getVendor(), transactionId);
        List<RelatedTransaction> employeeTxns = riskEvidenceRepository.findRelatedByEmployee(
                transaction.getEmployee(), transactionId);
        List<RelatedTransaction> categoryTxns = riskEvidenceRepository.findRelatedByCategory(
                transaction.getCategory(), transactionId);

        RelatedGroup vendorGroup = buildGroup(transaction.getVendor(), vendorTxns);
        RelatedGroup employeeGroup = buildGroup(transaction.getEmployee(), employeeTxns);
        RelatedGroup categoryGroup = buildGroup(transaction.getCategory(), categoryTxns);

        // Compute unique aggregate evidence summary across all related dimensions
        Map<String, RelatedTransaction> uniqueTxns = new LinkedHashMap<>();
        for (RelatedTransaction rt : vendorTxns) {
            uniqueTxns.putIfAbsent(rt.getTransactionId(), rt);
        }
        for (RelatedTransaction rt : employeeTxns) {
            uniqueTxns.putIfAbsent(rt.getTransactionId(), rt);
        }
        for (RelatedTransaction rt : categoryTxns) {
            uniqueTxns.putIfAbsent(rt.getTransactionId(), rt);
        }

        int totalCount = uniqueTxns.size();
        int totalFlagged = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (RelatedTransaction rt : uniqueTxns.values()) {
            if (rt.getAmount() != null) {
                totalAmount = totalAmount.add(rt.getAmount());
            }
            if (rt.getRiskScore() != null && rt.getRiskScore() >= 30) {
                totalFlagged++;
            }
        }

        EvidenceSummary summary = new EvidenceSummary(totalCount, totalFlagged, totalAmount);

        return new TransactionContext(
                transactionId,
                vendorGroup,
                employeeGroup,
                categoryGroup,
                summary
        );
    }

    private RelatedGroup buildGroup(String name, List<RelatedTransaction> transactions) {
        int count = transactions.size();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int highestRiskScore = 0;
        int flaggedCount = 0;

        for (RelatedTransaction rt : transactions) {
            if (rt.getAmount() != null) {
                totalAmount = totalAmount.add(rt.getAmount());
            }
            if (rt.getRiskScore() != null) {
                if (rt.getRiskScore() > highestRiskScore) {
                    highestRiskScore = rt.getRiskScore();
                }
                if (rt.getRiskScore() >= 30) {
                    flaggedCount++;
                }
            }
        }

        return new RelatedGroup(
                name,
                count,
                totalAmount,
                highestRiskScore,
                flaggedCount,
                transactions
        );
    }
}
