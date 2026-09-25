package com.vighnesh.service;

import org.springframework.stereotype.Service;
import repository.TransactionRepository;
import service.TransactionCsvReader;
import service.TransactionImportService;

/**
 * Spring-managed wrapper that exposes TransactionImportService
 * (a plain-Java class) as a Spring bean, following the same
 * factory pattern used by RiskEngineFactory.
 */
@Service
public class TransactionImportFacade {

    private final TransactionImportService delegate;

    public TransactionImportFacade(
            TransactionRepository transactionRepository,
            javax.sql.DataSource dataSource) {
        this.delegate = new TransactionImportService(
                new TransactionCsvReader(),
                transactionRepository,
                dataSource
        );
    }

    public int importFromCsv(String filePath) throws Exception {
        return delegate.importFromCsv(filePath);
    }
}
