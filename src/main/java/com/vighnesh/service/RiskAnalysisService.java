package com.vighnesh.service;

import com.vighnesh.exception.AnalysisRunNotFoundException;
import com.vighnesh.exception.TransactionNotFoundException;
import model.RiskAnalysisHistoryItem;
import model.RiskAnalysisHistoryPage;
import model.RiskAnalysisHistoryResponse;
import model.RiskAnalysisRun;
import model.RiskFinding;
import model.RiskReport;
import model.RiskSeverity;
import model.RiskSummary;
import model.RiskTransactionResponse;
import model.Transaction;
import model.RiskTransactionPage;
import repository.TransactionRepository;
import repository.RiskAnalysisRunRepository;
import repository.RiskFindingRepository;
import rule.DuplicateTransactionRule;
import rule.HighAmountRule;
import rule.UnusualTimeRule;
import service.RiskEngine;


import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RiskAnalysisService {

    private final TransactionRepository transactionRepository;
    private final RiskAnalysisRunRepository riskAnalysisRunRepository;
    private final RiskFindingRepository riskFindingRepository;

    public RiskAnalysisService(
            TransactionRepository transactionRepository,
            RiskAnalysisRunRepository riskAnalysisRunRepository,
            RiskFindingRepository riskFindingRepository) {

        this.transactionRepository = transactionRepository;
        this.riskAnalysisRunRepository =
                riskAnalysisRunRepository;
        this.riskFindingRepository =
                riskFindingRepository;
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

    public List<RiskTransactionResponse> analyzeAllTransactions()
            throws Exception {

        List<Transaction> transactions =
                transactionRepository.findAll();

        RiskEngine engine = new RiskEngine();

        engine.addRule(new HighAmountRule());
        engine.addRule(new UnusualTimeRule());

        engine.addDatasetRule(
                new DuplicateTransactionRule()
        );

        List<RiskTransactionResponse> results =
                new ArrayList<>();

        for (Transaction transaction : transactions) {

            RiskReport report =
                    engine.analyze(
                            transaction,
                            transactions
                    );

            results.add(
                    new RiskTransactionResponse(
                            transaction.getId(),
                            transaction.getVendor(),
                            transaction.getEmployee(),
                            transaction.getAmount(),
                            transaction.getCategory(),
                            report.getRiskScore(),
                            report.getRiskLevel(),
                            report.getFindings()
                    )
            );
        }

        return results;
    }


    public RiskTransactionPage analyzeTransactions(
            RiskSeverity riskLevel,
            Integer minScore,
            int page,
            int size)
            throws Exception {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be greater than or equal to 0"
            );
        }

        if (size <= 0) {
            throw new IllegalArgumentException(
                    "Size must be greater than 0"
            );
        }

        // Get all analyzed transactions
        List<RiskTransactionResponse> results =
                analyzeAllTransactions();

        // Filter by risk level
        if (riskLevel != null) {
            results = results.stream()
                    .filter(transaction ->
                            transaction.getRiskLevel() == riskLevel)
                    .collect(Collectors.toList());
        }

        // Filter by minimum score
        if (minScore != null) {
            results = results.stream()
                    .filter(transaction ->
                            transaction.getRiskScore() >= minScore)
                    .collect(Collectors.toList());
        }

        // Total number AFTER filtering
        long totalElements = results.size();

        // Calculate total pages
        int totalPages =
                totalElements == 0
                        ? 0
                        : (int) Math.ceil(
                                (double) totalElements / size
                        );

        // Calculate starting index
        int start = page * size;

        // Requested page is beyond available data
        if (start >= totalElements) {
            return new RiskTransactionPage(
                    List.of(),
                    page,
                    size,
                    totalElements,
                    totalPages
            );
        }

        // Calculate ending index
        int end =
                Math.min(start + size, results.size());

        List<RiskTransactionResponse> content =
                results.subList(start, end);

        return new RiskTransactionPage(
                content,
                page,
                size,
                totalElements,
                totalPages
        );
    }
    public void saveRiskFindings(
            long analysisRunId,
            String transactionId,
            RiskReport report)
            throws Exception {

        for (var finding : report.getFindings()) {

            riskFindingRepository.save(
                    analysisRunId,
                    transactionId,
                    finding
            );
        }
    }

    public RiskReport analyzeAndPersistTransaction(
            String transactionId)
            throws Exception {

        List<Transaction> transactions =
                transactionRepository.findAll();

        Transaction transaction =
                transactions.stream()
                        .filter(t ->
                                t.getId().equals(transactionId))
                        .findFirst()
                        .orElse(null);

        if (transaction == null) {

            throw new TransactionNotFoundException(
                    transactionId
            );
        }

        RiskEngine engine = new RiskEngine();

        engine.addRule(
                new HighAmountRule()
        );

        engine.addRule(
                new UnusualTimeRule()
        );

        engine.addDatasetRule(
                new DuplicateTransactionRule()
        );

        RiskReport report =
                engine.analyze(
                        transaction,
                        transactions
                );

        long analysisRunId =
                riskAnalysisRunRepository.save(
                        transactionId,
                        report.getRiskScore(),
                        report.getRiskLevel()
                );

        saveRiskFindings(
                analysisRunId,
                transactionId,
                report
        );

        return report;
    }

    public List<RiskFinding> getPersistedFindings(
            String transactionId)
            throws Exception {

        Transaction transaction =
                transactionRepository.findById(transactionId);

        if (transaction == null) {

            throw new TransactionNotFoundException(
                    transactionId
            );
        }

        RiskAnalysisRun latestRun =
                riskAnalysisRunRepository
                        .findLatestByTransactionId(
                                transactionId
                        );

        if (latestRun == null) {
            return List.of();
        }

        return riskFindingRepository.findByAnalysisRunId(
                latestRun.getId()
        );
    }

    public RiskAnalysisHistoryResponse getTransactionRiskHistory(
            String transactionId)
            throws Exception {

        Transaction transaction =
                transactionRepository.findById(transactionId);

        if (transaction == null) {
            throw new TransactionNotFoundException(
                    transactionId
            );
        }

        List<RiskAnalysisRun> runs =
                riskAnalysisRunRepository
                        .findByTransactionId(transactionId);

        List<RiskAnalysisHistoryItem> history =
                new ArrayList<>();

        for (RiskAnalysisRun run : runs) {

            List<RiskFinding> findings =
                    riskFindingRepository
                            .findByAnalysisRunId(
                                    run.getId()
                            );

            history.add(
                    new RiskAnalysisHistoryItem(
                            run.getId(),
                            run.getAnalyzedAt(),
                            run.getRiskScore(),
                            run.getRiskLevel(),
                            findings
                    )
            );
        }

        return new RiskAnalysisHistoryResponse(
                transactionId,
                history
        );
    }

    public RiskAnalysisHistoryItem getTransactionRiskHistoryRun(
            String transactionId,
            long analysisRunId)
            throws Exception {

        Transaction transaction =
                transactionRepository.findById(transactionId);

        if (transaction == null) {
            throw new TransactionNotFoundException(
                    transactionId
            );
        }

        RiskAnalysisRun run =
                riskAnalysisRunRepository
                        .findByIdAndTransactionId(
                                analysisRunId,
                                transactionId
                        );

        if (run == null) {
            throw new AnalysisRunNotFoundException(
                    analysisRunId
            );
        }

        List<RiskFinding> findings =
                riskFindingRepository.findByAnalysisRunId(
                        run.getId()
                );

        return new RiskAnalysisHistoryItem(
                run.getId(),
                run.getAnalyzedAt(),
                run.getRiskScore(),
                run.getRiskLevel(),
                findings
        );
    }

    public RiskAnalysisHistoryPage getTransactionRiskHistoryPage(
            String transactionId,
            int page,
            int size)
            throws Exception {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be greater than or equal to 0"
            );
        }

        if (size <= 0) {
            throw new IllegalArgumentException(
                    "Size must be greater than 0"
            );
        }

        Transaction transaction =
                transactionRepository.findById(transactionId);

        if (transaction == null) {
            throw new TransactionNotFoundException(
                    transactionId
            );
        }

        long totalElements =
                riskAnalysisRunRepository
                        .countByTransactionId(transactionId);

        List<RiskAnalysisRun> runs =
                riskAnalysisRunRepository.findByTransactionId(
                        transactionId,
                        page,
                        size
                );

        List<RiskAnalysisHistoryItem> history =
                new ArrayList<>();

        for (RiskAnalysisRun run : runs) {

            List<RiskFinding> findings =
                    riskFindingRepository.findByAnalysisRunId(
                            run.getId()
                    );

            history.add(
                    new RiskAnalysisHistoryItem(
                            run.getId(),
                            run.getAnalyzedAt(),
                            run.getRiskScore(),
                            run.getRiskLevel(),
                            findings
                    )
            );
        }

        return new RiskAnalysisHistoryPage(
                transactionId,
                history,
                page,
                size,
                totalElements
        );
    }
}