package com.vighnesh.service;

import com.vighnesh.exception.TransactionNotFoundException;
import model.RiskReport;
import model.RiskSeverity;
import model.RiskSummary;
import model.Transaction;
import repository.TransactionRepository;
import rule.DuplicateTransactionRule;
import rule.HighAmountRule;
import rule.UnusualTimeRule;
import service.RiskEngine;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RiskAnalysisService {

    private final TransactionRepository transactionRepository;

    public RiskAnalysisService(
            TransactionRepository transactionRepository) {

        this.transactionRepository = transactionRepository;
    }

    public RiskReport analyzeTransaction(String transactionId)
            throws Exception {

        List<Transaction> transactions =
                transactionRepository.findAll();

        Transaction transaction = transactions.stream()
                .filter(t -> t.getId().equals(transactionId))
                .findFirst()
                .orElse(null);

        if (transaction == null) {
            throw new TransactionNotFoundException(transactionId);
        }

        RiskEngine engine = new RiskEngine();

        engine.addRule(new HighAmountRule());
        engine.addRule(new UnusualTimeRule());

        engine.addDatasetRule(
                new DuplicateTransactionRule()
        );

        return engine.analyze(
                transaction,
                transactions
        );
    }

    public RiskSummary analyzeSummary()
            throws Exception {

        List<Transaction> transactions =
                transactionRepository.findAll();

        int low = 0;
        int medium = 0;
        int high = 0;
        int critical = 0;

        int totalFindings = 0;

        BigDecimal totalAmount = BigDecimal.ZERO;

        String highestRiskTransactionId = null;
        int highestRiskScore = -1;

        RiskEngine engine = new RiskEngine();

        engine.addRule(new HighAmountRule());
        engine.addRule(new UnusualTimeRule());

        engine.addDatasetRule(
                new DuplicateTransactionRule()
        );

        for (Transaction transaction : transactions) {

            totalAmount =
                    totalAmount.add(transaction.getAmount());

            RiskReport report =
                    engine.analyze(
                            transaction,
                            transactions
                    );

            totalFindings +=
                    report.getFindings().size();

            switch (report.getRiskLevel()) {

                case LOW -> low++;

                case MEDIUM -> medium++;

                case HIGH -> high++;

                case CRITICAL -> critical++;
            }

            if (report.getRiskScore() > highestRiskScore) {

                highestRiskScore =
                        report.getRiskScore();

                highestRiskTransactionId =
                        transaction.getId();
            }
        }

        return new RiskSummary(
                transactions.size(),
                totalAmount,
                low,
                medium,
                high,
                critical,
                totalFindings,
                highestRiskTransactionId,
                highestRiskScore
        );
    }
}