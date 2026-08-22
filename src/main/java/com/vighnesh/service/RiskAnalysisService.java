package com.vighnesh.service;

import com.vighnesh.exception.TransactionNotFoundException;
import model.RiskReport;
import model.Transaction;
import repository.TransactionRepository;
import rule.DuplicateTransactionRule;
import rule.HighAmountRule;
import rule.UnusualTimeRule;
import service.RiskEngine;

import org.springframework.stereotype.Service;

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
}